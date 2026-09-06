package com.shop.checkout.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.shop.cart.entity.Cart;
import com.shop.cart.repository.CartItemRepository;
import com.shop.cart.repository.CartRepository;
import com.shop.checkout.domain.DeliveryMethod;
import com.shop.checkout.domain.DraftStatus;
import com.shop.checkout.dto.CheckoutConfirmContext;
import com.shop.checkout.dto.CheckoutConfirmFailedItem;
import com.shop.checkout.dto.CheckoutConfirmFailureResponse;
import com.shop.checkout.dto.CheckoutConfirmResponse;
import com.shop.checkout.entity.CheckoutDraft;
import com.shop.checkout.entity.CheckoutDraftItem;
import com.shop.checkout.exception.CheckoutConfirmConflictException;
import com.shop.checkout.lines.CheckoutLineFailures;
import com.shop.checkout.lines.CheckoutLineHandler;
import com.shop.checkout.lines.CheckoutLineHandlerRegistry;
import com.shop.checkout.repository.CheckoutDraftRepository;
import com.shop.common.identity.UserIdentity;
import com.shop.log.point.dto.PointLogDto;
import com.shop.log.point.entity.PointLogActorType;
import com.shop.log.point.service.PointLogService;
import com.shop.offline.product.service.OfflineOnlineShippingQuantityAdjuster;
import com.shop.order.entity.OrderInfo;
import com.shop.order.entity.OrderProduct;
import com.shop.order.enums.DeliveryCompany;
import com.shop.order.enums.OrderStatus;
import com.shop.order.event.OrderCreatedEvent;
import com.shop.order.point.OrderEarnedPointCalculator;
import com.shop.order.repository.OrderInfoRepository;
import com.shop.order.repository.OrderProductRepository;
import com.shop.order.support.OrderInfoPersonalDataCodec;
import com.shop.search.dto.enums.ProductTableEnum;
import com.shop.search.entity.ProductSearchMap;
import com.shop.search.repository.map.ProductSearchMapRepository;
import com.shop.search.service.ProductSearchMapStockSyncPublisher;
import com.shop.user.entity.User;
import com.shop.user.repository.UserRepository;
import com.shop.user.service.user.UserService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
@Slf4j
@Service
@RequiredArgsConstructor
public class CheckoutConfirmServiceImpl implements CheckoutConfirmService {

    private static final String PSM_STOCK_LOG = "[PSM-STOCK]";

    // 결제 상태들
    private static final String PAYMENT_DIRECT = "PAYMENT_DIRECT"; // 직접 결제

    private final CheckoutDraftRepository checkoutDraftRepository;
    private final ProductSearchMapRepository productSearchMapRepository;
    private final CheckoutLineHandlerRegistry lineHandlerRegistry;
    private final OrderInfoRepository orderInfoRepository;
    private final OrderProductRepository orderProductRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final UserService userService;
    private final UserRepository userRepository;
    private final ProductSearchMapStockSyncPublisher productSearchMapStockSyncPublisher;
    private final PointLogService pointLogService;
    private final OrderInfoPersonalDataCodec orderInfoPersonalDataCodec;
    private final ApplicationEventPublisher eventPublisher;
    private final CheckoutDraftValidationService checkoutDraftValidationService;
    private final OfflineOnlineShippingQuantityAdjuster offlineOnlineShippingQuantityAdjuster;

    //체크아웃 확정 로직
    @Override
    @Transactional
    public ResponseEntity<?> confirm(String draftPublicId, UserIdentity identity) {
        return finalizeAfterPayment(draftPublicId, identity, null);
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<?> validateDraft(String draftPublicId, UserIdentity identity) {
        return checkoutDraftValidationService.validateDraft(draftPublicId, identity);
    }

    @Override
    @Transactional
    public ResponseEntity<?> finalizeAfterPayment(
            String draftPublicId, UserIdentity identity, CheckoutConfirmContext paymentContext) {
        CheckoutDraft draft = checkoutDraftRepository.findByPublicIdForUpdate(draftPublicId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "DRAFT_NOT_FOUND"));

        assertOwner(draft, identity);

        if (draft.getStatus() == DraftStatus.CONFIRMED && draft.getConfirmedOrderId() != null) {
            OrderInfo existing = orderInfoRepository.findById(draft.getConfirmedOrderId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "ORDER_MISSING"));
            return ResponseEntity.ok(toConfirmResponse(existing));
        }

        if (draft.getStatus() == DraftStatus.REPLACED) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(simpleFailure("DRAFT_REPLACED", "Draft was replaced by a newer checkout."));
        }

        if (draft.getStatus() == DraftStatus.EXPIRED
                || (draft.getStatus() == DraftStatus.READY && LocalDateTime.now().isAfter(draft.getExpiresAt()))) {
            if (draft.getStatus() == DraftStatus.READY) {
                draft.setStatus(DraftStatus.EXPIRED);
            }
            return ResponseEntity.status(HttpStatus.GONE).body(simpleFailure("DRAFT_EXPIRED", "Checkout draft expired."));
        }

        if (draft.getStatus() != DraftStatus.READY) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(simpleFailure("DRAFT_NOT_CONFIRMABLE", "Draft cannot be confirmed."));
        }

        CheckoutConfirmFailureResponse recipientInfoError = validateRequiredRecipientInfo(draft);
        if (recipientInfoError != null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(recipientInfoError);
        }

        draft.getItems().size();

        List<CheckoutConfirmFailedItem> failures = validateLineItems(draft);
        if (!failures.isEmpty()) {
            String code = aggregateFailureCode(failures);
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(CheckoutConfirmFailureResponse.builder()
                            .code(code)
                            .message("Checkout validation failed.")
                            .failedItems(failures)
                            .build());
        }

        CheckoutConfirmFailureResponse integrityError = verifyAmountInvariants(draft);
        if (integrityError != null) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(integrityError);
        }

        List<CheckoutDraftItem> sortedForStock = draft.getItems().stream()
                .sorted(stockDeductionComparator())
                .collect(Collectors.toList());

        for (CheckoutDraftItem line : sortedForStock) {
            ProductTableEnum table = ProductTableEnum.valueOf(line.getTableName());
            CheckoutLineHandler handler = lineHandlerRegistry.require(table);
            boolean reserved = handler.tryReserveStock(line);
            log.info(
                    "{} checkout stock reserved table={} searchMapId={} productPublicId={} sourceId={} "
                            + "qty={} reserved={}",
                    PSM_STOCK_LOG,
                    table,
                    line.getSearchMapId(),
                    line.getProductPublicId(),
                    line.getSourceId(),
                    line.getQuantity(),
                    reserved);
            if (!reserved) {
                CheckoutConfirmFailedItem fi = handler.buildFailureAfterReserveMiss(line);
                throw new CheckoutConfirmConflictException(
                        CheckoutConfirmFailureResponse.builder()
                                .code(fi.getReason())
                                .message("Concurrency conflict while reserving stock.")
                                .failedItems(List.of(fi))
                                .build());
            }
            publishSearchMapStockSync(table, line, handler);
            int qty = line.getQuantity() != null ? line.getQuantity().intValue() : 0;
            if (qty > 0) {
                offlineOnlineShippingQuantityAdjuster.increaseForOrderLine(table, line.getSourceId(), qty);
            }
        }

        long usedPoints = draft.getUsedPointAmount() != null ? draft.getUsedPointAmount().longValue() : 0L;
        Long pointDeductUserId = null;
        User pointDeductUser = null;
        long pointBeforeDeduct = 0L;
        if (identity.isUser() && usedPoints > 0) {
            Long uid = userService.findUserIdByPublicId(identity.getUserPublicId()).orElseThrow();
            pointDeductUser = userRepository.findById(uid).orElse(null);
            if (pointDeductUser != null) {
                pointDeductUserId = uid;
                pointBeforeDeduct = pointDeductUser.getPoint() != null ? pointDeductUser.getPoint() : 0L;
            }
            int rows = userRepository.deductPoints(uid, usedPoints);
            if (rows != 1) {
                throw new CheckoutConfirmConflictException(
                        CheckoutConfirmFailureResponse.builder()
                                .code("INSUFFICIENT_POINTS")
                                .message("Point balance changed.")
                                .failedItems(List.of())
                                .build());
            }
        }

        LocalDateTime now = LocalDateTime.now();
        String guestCode = identity.isGuest()
                ? String.format("%06d", ThreadLocalRandom.current().nextInt(0, 1_000_000))
                : null;

        BigDecimal total = draft.getTotalAmount();
        BigDecimal delivery = draft.getDeliveryFee();
        BigDecimal usedPts = draft.getUsedPointAmount();
        BigDecimal productSubtotal = draft.getSubtotalAmount() != null ? draft.getSubtotalAmount() : BigDecimal.ZERO;

        // 달러화 결제 시 총 결제 금액을 달러로 환산하지만, 추후 반영 예정이므로 주석처리.
        // if(draft.getPaymentCurrency().equals("USD")) {
        //     total = total.divide(draft.getPaymentCurrencyRate(), 2, RoundingMode.HALF_UP);
        // }

        Long userIdPk = draft.getUserId();
        long orderLineCount = draft.getItems().size();
        long totalQuantity = draft.getItems().stream().mapToLong(CheckoutDraftItem::getQuantity).sum();

        String orderStatus = paymentContext != null && paymentContext.getOrderStatus() != null
                ? paymentContext.getOrderStatus()
                : OrderStatus.ORDER_PENDING.getValue();
        String paymentStatus = paymentContext != null && paymentContext.getPaymentStatus() != null
                ? paymentContext.getPaymentStatus()
                : PAYMENT_DIRECT;
        String paymentMethod = paymentContext != null && paymentContext.getPaymentMethod() != null
                ? paymentContext.getPaymentMethod()
                : draft.getPaymentMethod();
        String pgTransactionId = paymentContext != null ? paymentContext.getPgTransactionId() : null;

        OrderInfo order = OrderInfo.builder()
                .guest(identity.isGuest())
                .userId(userIdPk)
                .recipientName(draft.getRecipientName())
                .recipientAddress(orderInfoPersonalDataCodec.encrypt(draft.getRecipientAddress()))
                .recipientAddressDetail(encryptIfPresent(draft.getRecipientAddressDetail()))
                .recipientPhone(orderInfoPersonalDataCodec.encrypt(draft.getRecipientPhone()))
                .recipientEmail(orderInfoPersonalDataCodec.encrypt(draft.getRecipientEmail()))
                .postalCode(draft.getRecipientPostalCode())
                .orderRequest(draft.getOrderRequest())
                .orderStatus(orderStatus)
                .paymentStatus(paymentStatus)
                .paymentCurrency("KRW")
                .paymentCurrencyRateSnapshot(draft.getPaymentCurrencyRate())
                .settleKrwAmount(computeSettleKrwAmount(draft))
                .totalProductAmount(productSubtotal)
                .totalPaymentAmount(total)
                .usedPointAmount(usedPts)
                .actualPaymentAmount(total.max(BigDecimal.ZERO))
                .deliveryFee(delivery)
                .paymentDate(now)
                .paymentApprovedAt(now)
                .guestVerificationCode(guestCode)
                .orderLineCount(orderLineCount)
                .totalQuantity(totalQuantity)
                .paymentMethod(paymentMethod)
                .pgTransactionId(pgTransactionId)
                .deliveryCompany(resolveDeliveryCompany(draft.getDeliveryMethod()).name())
                .build();

        orderInfoRepository.save(order);

        if (pointDeductUserId != null && pointDeductUser != null && usedPoints > 0) {
            pointLogService.savePointLog(PointLogDto.builder()
                    .userId(pointDeductUserId)
                    .userName(pointDeductUser.getName())
                    .changedPoint(-usedPoints)
                    .beforePoint(pointBeforeDeduct)
                    .afterPoint(pointBeforeDeduct - usedPoints)
                    .changeReason("ORDER_USE_POINTS:" + order.getId())
                    .actorType(PointLogActorType.SYSTEM)
                    .actorDetail(null)
                    .actionDate(now)
                    .isSuccess(true)
                    .build());
        }

        List<OrderProduct> savedOrderProducts = new ArrayList<>();
        for (CheckoutDraftItem line : draft.getItems()) {
            ProductTableEnum table = ProductTableEnum.valueOf(line.getTableName());
            OrderProduct op = lineHandlerRegistry.require(table).buildOrderProduct(order.getId(), line);
            orderProductRepository.save(op);
            savedOrderProducts.add(op);
        }

        long totalEarnedPts = OrderEarnedPointCalculator.totalEarnedPoints(savedOrderProducts);
        order.setEarnedPointAmount(totalEarnedPts);
        orderInfoRepository.save(order);

        draft.setStatus(DraftStatus.CONFIRMED);
        draft.setConfirmedOrderId(order.getId());
        checkoutDraftRepository.save(draft);

        removeMatchingCartLines(draft);

        order = orderInfoRepository.findById(order.getId()).orElse(order);
        log.info("주문 확정 완료, 알림 이벤트 발행: orderId={}", order.getId());
        eventPublisher.publishEvent(new OrderCreatedEvent(order.getId(), order.getUserId()));
        return ResponseEntity.ok(toConfirmResponse(order));
    }

    //검색 맵 스톡 동기화 로직
    private void publishSearchMapStockSync(
            ProductTableEnum table,
            CheckoutDraftItem line,
            CheckoutLineHandler handler) {
        switch (table) {
            case CARD_PRODUCT -> {
                long cardProductId = handler.stockDeductionEntityId(line);
                log.info(
                        "{} checkout schedule stock sync table=CARD_PRODUCT searchMapId={} productPublicId={} "
                                + "cardProductId={}",
                        PSM_STOCK_LOG,
                        line.getSearchMapId(),
                        line.getProductPublicId(),
                        cardProductId);
                productSearchMapRepository.findById(line.getSearchMapId()).ifPresentOrElse(
                        map -> log.info(
                                "{} checkout searchMap row searchMapId={} mapProductId={} mapInStock={} "
                                        + "matchesLineProductPublicId={}",
                                PSM_STOCK_LOG,
                                map.getId(),
                                map.getProductId(),
                                map.getInStock(),
                                Objects.equals(map.getProductId(), line.getProductPublicId())),
                        () -> log.warn(
                                "{} checkout searchMap row not found searchMapId={}",
                                PSM_STOCK_LOG,
                                line.getSearchMapId()));
                productSearchMapStockSyncPublisher.publishCardProductStockChanged(cardProductId);
            }
            case SEALED_PRODUCT -> {
                long sealedProductId = handler.stockDeductionEntityId(line);
                log.info(
                        "{} checkout schedule stock sync table=SEALED_PRODUCT searchMapId={} productPublicId={} "
                                + "sealedProductId={}",
                        PSM_STOCK_LOG,
                        line.getSearchMapId(),
                        line.getProductPublicId(),
                        sealedProductId);
                productSearchMapStockSyncPublisher.publishSealedProductStockChanged(sealedProductId);
            }
            case MANUAL_PRODUCT -> {
                log.info(
                        "{} checkout schedule stock sync table=MANUAL_PRODUCT searchMapId={} productPublicId={} "
                                + "manualProductId={}",
                        PSM_STOCK_LOG,
                        line.getSearchMapId(),
                        line.getProductPublicId(),
                        line.getSourceId());
                productSearchMapStockSyncPublisher.publishManualProductStockChanged(line.getSourceId());
            }
            case SUPPLY -> {
                log.info(
                        "{} checkout schedule stock sync table=SUPPLY searchMapId={} productPublicId={} "
                                + "supplyId={}",
                        PSM_STOCK_LOG,
                        line.getSearchMapId(),
                        line.getProductPublicId(),
                        line.getSourceId());
                productSearchMapStockSyncPublisher.publishSupplyStockChanged(line.getSourceId());
            }
            default -> log.info(
                    "{} checkout stock sync skipped unsupported table={} searchMapId={}",
                    PSM_STOCK_LOG,
                    table,
                    line.getSearchMapId());
        }
    }

    //재고 차감 순서 고정용 비교기
    private Comparator<CheckoutDraftItem> stockDeductionComparator() {
        return Comparator
                .comparingInt((CheckoutDraftItem line) -> ProductTableEnum.valueOf(line.getTableName()).ordinal())
                .thenComparingLong(line -> lineHandlerRegistry
                        .require(ProductTableEnum.valueOf(line.getTableName()))
                        .stockDeductionEntityId(line));
    }

    //장바구니 중복 라인 제거 로직
    private void removeMatchingCartLines(CheckoutDraft draft) {
        Cart cart = loadCart(draft);
        if (cart == null) {
            return;
        }
        for (CheckoutDraftItem snap : draft.getItems()) {
            cartItemRepository.findById(snap.getCartItemId()).ifPresent(actual -> {
                if (!Objects.equals(actual.getCart().getId(), cart.getId())) {
                    return;
                }
                if (!Objects.equals(actual.getQuantity(), snap.getCartItemQuantity())) {
                    return;
                }
                if (snap.getCartItemUpdatedAt() == null && actual.getUpdatedAt() == null) {
                    cartItemRepository.delete(actual);
                    return;
                }
                if (snap.getCartItemUpdatedAt() == null || actual.getUpdatedAt() == null) {
                    return;
                }
                if (snap.getCartItemUpdatedAt().equals(actual.getUpdatedAt())) {
                    cartItemRepository.delete(actual);
                }
            });
        }
    }

    //장바구니 로드 로직
    private Cart loadCart(CheckoutDraft draft) {
        if (draft.getUserId() != null) {
            return cartRepository.findByUserId(draft.getUserId()).orElse(null);
        }
        if (draft.getGuestId() != null) {
            return cartRepository.findByGuestId(draft.getGuestId()).orElse(null);
        }
        return null;
    }

    /**
     * validateLineItems() 통과 후 호출. 단가가 DB와 일치함이 보장된 상태에서
     * 라인 총액·헤더 합계·포인트 상한의 불변식을 검산한다.
     *
     * <pre>
     * ① 각 라인: snapshotTotalPrice == snapshotUnitPrice × quantity
     * ② subtotal: Σ(snapshotTotalPrice) == draft.subtotalAmount
     * ③ total:    max(0, subtotalAmount + deliveryFee - usedPointAmount) == totalAmount
     * ④ 포인트 상한: usedPointAmount ≤ subtotalAmount + deliveryFee
     * </pre>
     */
    private CheckoutConfirmFailureResponse verifyAmountInvariants(CheckoutDraft draft) {
        BigDecimal subtotalFromLines = BigDecimal.ZERO;

        for (CheckoutDraftItem line : draft.getItems()) {
            BigDecimal expectedLineTotal = line.getSnapshotUnitPrice()
                    .multiply(BigDecimal.valueOf(line.getQuantity()));
            if (expectedLineTotal.compareTo(line.getSnapshotTotalPrice()) != 0) {
                return amountIntegrityError("라인 총액이 단가 × 수량과 일치하지 않습니다.");
            }
            subtotalFromLines = subtotalFromLines.add(line.getSnapshotTotalPrice());
        }

        BigDecimal subtotal = draft.getSubtotalAmount() != null ? draft.getSubtotalAmount() : BigDecimal.ZERO;
        if (subtotalFromLines.compareTo(subtotal) != 0) {
            return amountIntegrityError("라인 합계가 주문 소계와 일치하지 않습니다.");
        }

        BigDecimal delivery = draft.getDeliveryFee() != null ? draft.getDeliveryFee() : BigDecimal.ZERO;
        BigDecimal usedPts = draft.getUsedPointAmount() != null ? draft.getUsedPointAmount() : BigDecimal.ZERO;
        BigDecimal total = draft.getTotalAmount() != null ? draft.getTotalAmount() : BigDecimal.ZERO;

        if (usedPts.compareTo(BigDecimal.ZERO) < 0) {
            return amountIntegrityError("사용 포인트가 음수입니다.");
        }
        if (usedPts.compareTo(subtotal.add(delivery)) > 0) {
            return amountIntegrityError("사용 포인트가 결제 가능 금액을 초과합니다.");
        }

        BigDecimal expectedTotal = subtotal.add(delivery).subtract(usedPts).max(BigDecimal.ZERO);
        if (expectedTotal.compareTo(total) != 0) {
            return amountIntegrityError("최종 결제 금액이 소계 + 배송비 - 포인트와 일치하지 않습니다.");
        }

        return null;
    }

    //금액 불변식 위반 오류 응답 생성 로직-즉 검증 실패 시 응답 생성
    private CheckoutConfirmFailureResponse amountIntegrityError(String message) {
        return CheckoutConfirmFailureResponse.builder()
                .code("AMOUNT_INTEGRITY_ERROR")
                .message(message)
                .failedItems(List.of())
                .build();
    }

    //라인 검증 로직
    private List<CheckoutConfirmFailedItem> validateLineItems(CheckoutDraft draft) {
        List<CheckoutConfirmFailedItem> failures = new ArrayList<>();
        List<CheckoutDraftItem> lines = new ArrayList<>(draft.getItems());
        lines.sort(Comparator.comparing(CheckoutDraftItem::getProductPublicId));

        for (CheckoutDraftItem line : lines) {
            CheckoutConfirmFailedItem failure = validateDraftLine(line);
            if (failure != null) {
                failures.add(failure);
            }
        }
        return failures;
    }

    //라인 검증 로직
    private CheckoutConfirmFailedItem validateDraftLine(CheckoutDraftItem line) {
        ProductSearchMap map = productSearchMapRepository.findById(line.getSearchMapId()).orElse(null);
        if (map == null || !Boolean.TRUE.equals(map.getIsVisible())) {
            return CheckoutLineFailures.unavailable(line, 0L);
        }
        if (!map.getTableName().name().equals(line.getTableName())
                || !Objects.equals(map.getSourceId(), line.getSourceId())
                || !Objects.equals(map.getProductId(), line.getProductPublicId())) {
            return CheckoutLineFailures.unavailable(line, 0L);
        }

        Optional<ProductTableEnum> tableOpt = parseProductTable(line.getTableName());
        if (tableOpt.isEmpty()) {
            return CheckoutLineFailures.unsupportedTable(line);
        }

        CheckoutLineHandler handler = lineHandlerRegistry.find(tableOpt.get()).orElse(null);
        if (handler == null) {
            return CheckoutLineFailures.unsupportedTable(line);
        }

        return handler.validate(line, map);
    }

    //제품 테이블 파싱 로직
    private static Optional<ProductTableEnum> parseProductTable(String tableName) {
        if (tableName == null || tableName.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(ProductTableEnum.valueOf(tableName));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    //오류 코드 집계 로직
    private String aggregateFailureCode(List<CheckoutConfirmFailedItem> failures) {
        if (failures.isEmpty()) {
            return "UNKNOWN";
        }
        String first = failures.get(0).getReason();
        boolean allSame = failures.stream().allMatch(f -> Objects.equals(first, f.getReason()));
        return allSame ? first : "MULTIPLE_FAILURES";
    }

    private CheckoutConfirmFailureResponse simpleFailure(String code, String message) {
        return CheckoutConfirmFailureResponse.builder()
                .code(code)
                .message(message)
                .failedItems(List.of())
                .build();
    }

    private CheckoutConfirmFailureResponse validateRequiredRecipientInfo(CheckoutDraft draft) {
        if (isBlank(draft.getRecipientName())) {
            return simpleFailure("RECIPIENT_NAME_REQUIRED", "Recipient name is required.");
        }
        if (isBlank(draft.getRecipientEmail())) {
            return simpleFailure("RECIPIENT_EMAIL_REQUIRED", "Recipient email is required.");
        }
        if (isBlank(draft.getRecipientPhone())) {
            return simpleFailure("RECIPIENT_PHONE_REQUIRED", "Recipient phone is required.");
        }
        return null;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static DeliveryCompany resolveDeliveryCompany(DeliveryMethod method) {
        return method == DeliveryMethod.DELIVERY
                ? DeliveryCompany.CJ_LOGISTICS
                : DeliveryCompany.STORE_PICKUP;
    }

    private String encryptIfPresent(String value) {
        if (isBlank(value)) {
            return null;
        }
        return orderInfoPersonalDataCodec.encrypt(value);
    }

    //정산 원화 금액 계산 로직
    private static long computeSettleKrwAmount(CheckoutDraft draft) {
        BigDecimal total = draft.getTotalAmount() != null ? draft.getTotalAmount() : BigDecimal.ZERO;
        // USD 환율 흐름 배제 — KRW만 사용
        // BigDecimal rate = draft.getPaymentCurrencyRate() != null ? draft.getPaymentCurrencyRate() : BigDecimal.ONE;
        // if (draft.getPaymentCurrency().equals("USD")) {
        //     return total.multiply(rate).setScale(0, RoundingMode.HALF_UP).longValue();
        // }
        return total.setScale(0, RoundingMode.HALF_UP).longValue();
    }

    //체크아웃 소유자 검증 로직
    private void assertOwner(CheckoutDraft draft, UserIdentity identity) {
        if (identity.isUser()) {
            Long uid = userService.findUserIdByPublicId(identity.getUserPublicId()).orElse(null);
            if (uid == null || !uid.equals(draft.getUserId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "DRAFT_FORBIDDEN");
            }
        } else {
            if (identity.getGuestId() == null || !identity.getGuestId().equals(draft.getGuestId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "DRAFT_FORBIDDEN");
            }
        }
    }

    //체크아웃 확정 응답 생성 로직
    private CheckoutConfirmResponse toConfirmResponse(OrderInfo order) {
        return CheckoutConfirmResponse.builder()
                .orderId(order.getId())
                .paymentStatus(order.getPaymentStatus())
                .confirmedAt(order.getPaymentApprovedAt() != null ? order.getPaymentApprovedAt() : order.getPaymentDate())
                .guestVerificationCode(order.getGuestVerificationCode())
                .build();
    }
}
