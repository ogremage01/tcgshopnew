package com.shop.checkout.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.shop.cart.entity.Cart;
import com.shop.cart.entity.CartItem;
import com.shop.cart.repository.CartItemRepository;
import com.shop.cart.repository.CartRepository;
import com.shop.checkout.domain.DeliveryMethod;
import com.shop.checkout.domain.DraftStatus;
import com.shop.checkout.dto.CheckoutDraftCreateResponse;
import com.shop.checkout.dto.CheckoutDraftItemResponse;
import com.shop.checkout.dto.CheckoutDraftResponse;
import com.shop.checkout.dto.PatchCheckoutDraftRequest;
import com.shop.checkout.entity.CheckoutDraft;
import com.shop.checkout.entity.CheckoutDraftItem;
import com.shop.checkout.pricing.CheckoutShowingPriceCalculator;
import com.shop.checkout.repository.CheckoutDraftRepository;
import com.shop.common.identity.UserIdentity;
import com.shop.common.util.UlidGenerator;
import com.shop.order.entity.OrderConfig;
import com.shop.order.repository.OrderConfigRepository;
import com.shop.product.dto.ProductItemDto;
import com.shop.product.entity.manualProduct.ManualProduct;
import com.shop.product.repository.manualProduct.ManualProductRepository;
import com.shop.search.dto.enums.ProductTableEnum;
import com.shop.search.entity.ProductSearchMap;
import com.shop.search.repository.map.ProductSearchMapRepository;
import com.shop.search.service.ProductSearchMapService;
import com.shop.user.service.user.UserService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CheckoutDraftServiceImpl implements CheckoutDraftService {

    private static final int DRAFT_TTL_MINUTES = 30;
    private static final String CFG_SHIPPING_FEE = "SHIPPING_FEE";
    private static final String CFG_FREE_SHIPPING_THRESHOLD = "FREE_SHIPPING_THRESHOLD";
    private static final BigDecimal DEFAULT_SHIPPING_FEE = new BigDecimal("3000");
    private static final String EVENT_TICKET_TYPE_NORMALIZED = "event-ticket";
    private static final String PAYMENT_CURRENCY_KRW = "KRW";
    /** USD 환율 미사용. DB NOT NULL 컬럼용 KRW 1:1 */
    private static final BigDecimal KRW_PAYMENT_CURRENCY_RATE = BigDecimal.ONE;
    // private static final String CFG_US_CURRENCY_RATE = "us_currency_rate";

    private final CheckoutDraftRepository checkoutDraftRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductSearchMapService productSearchMapService;
    private final ProductSearchMapRepository productSearchMapRepository;
    private final OrderConfigRepository orderConfigRepository;
    // private final PriceConfigService priceConfigService;
    private final CheckoutShowingPriceCalculator checkoutShowingPriceCalculator;
    private final UserService userService;
    private final ManualProductRepository manualProductRepository;

    //드래프트 생성
    @Override
    @Transactional
    public CheckoutDraftCreateResponse createDraft(UserIdentity identity) {
        Long userIdPk = resolveUserIdPk(identity);
        String guestId = identity.isGuest() ? identity.getGuestId() : null;

        if (userIdPk != null) {
            checkoutDraftRepository.updateStatusByUserIdAndStatus(userIdPk, DraftStatus.READY, DraftStatus.REPLACED);
        } else if (guestId != null) {
            checkoutDraftRepository.updateStatusByGuestIdAndStatus(guestId, DraftStatus.READY, DraftStatus.REPLACED);
        }

        Cart cart = loadCartOrThrow(userIdPk, guestId);
        List<CartItem> cartItems = cartItemRepository.findByCart(cart);
        if (cartItems.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "EMPTY_CART");
        }

        List<CartItem> sorted = cartItems.stream()
                .sorted(Comparator.comparing(CartItem::getSearchMapId))
                .collect(Collectors.toList());

        // USD 환율 흐름 배제 — 결제는 KRW만 사용
        // String primaryGame = sorted.stream()
        //         .map(line -> productSearchMapRepository.findById(line.getSearchMapId()).orElse(null))
        //         .filter(Objects::nonNull)
        //         .filter(CheckoutDraftServiceImpl::isCardCatalogTable)
        //         .map(ProductSearchMap::getGame)
        //         .filter(g -> g != null && !g.isBlank())
        //         .findFirst()
        //         .orElse(null);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plusMinutes(DRAFT_TTL_MINUTES);

        BigDecimal subtotal = BigDecimal.ZERO;
        CheckoutDraft draft = CheckoutDraft.builder()
                .publicId(UlidGenerator.nextUlid())
                .userId(userIdPk)
                .guestId(guestId)
                .status(DraftStatus.READY)
                .deliveryMethod(DeliveryMethod.STORE_PICKUP)
                .subtotalAmount(BigDecimal.ZERO)
                .deliveryFee(BigDecimal.ZERO)
                .usedPointAmount(BigDecimal.ZERO)
                .totalAmount(BigDecimal.ZERO)
                .totalProductAmount(BigDecimal.ZERO)
                .expiresAt(expiresAt)
                .paymentCurrency(PAYMENT_CURRENCY_KRW)
                .paymentCurrencyRate(KRW_PAYMENT_CURRENCY_RATE)
                // .paymentCurrencyRate(resolveUsCurrencyRate(primaryGame))
                .settleKrwAmount(0L)
                .paymentMethod("DIRECT")
                .build();

        for (CartItem line : sorted) {
            ProductSearchMap map = productSearchMapRepository.findById(line.getSearchMapId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "PRODUCT_UNAVAILABLE"));
            if (!Boolean.TRUE.equals(map.getIsVisible())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "PRODUCT_UNAVAILABLE");
            }
            assertCheckoutSupportedProductTable(map);

            ProductItemDto dto = productSearchMapService.getProductItemDtoBySearchMapId(line.getSearchMapId());
            if (dto.getCurrentVisibleStock() == null || dto.getCurrentVisibleStock() < line.getQuantity()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "OUT_OF_STOCK");
            }
            if (dto.getPrice() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "PRODUCT_UNAVAILABLE");
            }

            BigDecimal unit = checkoutShowingPriceCalculator.toBigDecimal(dto.getPrice());
            BigDecimal lineTotal = unit.multiply(BigDecimal.valueOf(line.getQuantity())).setScale(0, RoundingMode.HALF_UP);
            subtotal = subtotal.add(lineTotal);

            String imageUrlEn = dto.getImageUrlEn();
            String imageUrlKo = dto.getImageUrlKo();
            String imageUrl = firstNonBlank(imageUrlEn, imageUrlKo);

            CheckoutDraftItem item = CheckoutDraftItem.builder()
                    .draft(draft)
                    .cartItemId(line.getId())
                    .cartItemQuantity(line.getQuantity())
                    .cartItemUpdatedAt(line.getUpdatedAt())
                    .searchMapId(line.getSearchMapId())
                    .tableName(map.getTableName().name())
                    .sourceId(map.getSourceId())
                    .productPublicId(map.getProductId())
                    .productType(Objects.toString(dto.getProductType(), ""))
                    .productNameEn(dto.getProductNameEn())
                    .productNameKo(dto.getProductNameKo())
                    .imageUrl(imageUrl)
                    .imageUrlEn(imageUrlEn)
                    .imageUrlKo(imageUrlKo)
                    .snapshotUnitPrice(unit)
                    .quantity(line.getQuantity())
                    .snapshotTotalPrice(lineTotal)
                    .snapshotPointAmount(dto.getSaveAmount())
                    .build();
            draft.getItems().add(item);
        }

        draft.setSubtotalAmount(subtotal.setScale(0, RoundingMode.HALF_UP));
        draft.setTotalProductAmount(draft.getSubtotalAmount());
        BigDecimal deliveryFee = computeDeliveryFee(draft.getTotalProductAmount(), draft.getDeliveryMethod());
        draft.setDeliveryFee(deliveryFee);
        draft.setUsedPointAmount(BigDecimal.ZERO);
        draft.setTotalAmount(draft.getSubtotalAmount().add(deliveryFee).setScale(0, RoundingMode.HALF_UP));
        syncSettleKrwAmount(draft);

        checkoutDraftRepository.save(draft);
        return CheckoutDraftCreateResponse.builder().publicId(draft.getPublicId()).build();
    }

    //드래프트 조회
    @Override
    @Transactional(readOnly = true)
    public CheckoutDraftResponse getDraft(String publicId, UserIdentity identity) {
        CheckoutDraft draft = checkoutDraftRepository.findByPublicId(publicId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "DRAFT_NOT_FOUND"));
        assertOwner(draft, identity);
        draft = refreshExpiryIfNeeded(draft);

        if (draft.getStatus() == DraftStatus.REPLACED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "DRAFT_REPLACED");
        }
        if (draft.getStatus() == DraftStatus.EXPIRED) {
            throw new ResponseStatusException(HttpStatus.GONE, "DRAFT_EXPIRED");
        }

        return toResponse(draft);
    }

    //드래프트 수정
    @Override
    @Transactional
    public CheckoutDraftResponse patchDraft(String publicId, PatchCheckoutDraftRequest request, UserIdentity identity) {
        CheckoutDraft draft = checkoutDraftRepository.findByPublicId(publicId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "DRAFT_NOT_FOUND"));
        assertOwner(draft, identity);
        draft = refreshExpiryIfNeededMutable(draft);

        if (draft.getStatus() != DraftStatus.READY) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "DRAFT_NOT_PATCHABLE");
        }

        if (request.getDeliveryMethod() != null) {
            draft.setDeliveryMethod(request.getDeliveryMethod());
        }
        if (request.getRecipientName() != null) {
            draft.setRecipientName(request.getRecipientName());
        }
        if (request.getRecipientAddress() != null) {
            draft.setRecipientAddress(request.getRecipientAddress());
        }
        if (request.getRecipientAddressDetail() != null) {
            draft.setRecipientAddressDetail(request.getRecipientAddressDetail());
        }
        if (request.getRecipientPostalCode() != null) {
            draft.setRecipientPostalCode(request.getRecipientPostalCode());
        }
        if (request.getRecipientPhone() != null) {
            draft.setRecipientPhone(request.getRecipientPhone());
        }
        if (request.getRecipientEmail() != null) {
            draft.setRecipientEmail(request.getRecipientEmail());
        }
        if (request.getOrderRequest() != null) {
            draft.setOrderRequest(request.getOrderRequest());
        }
        // USD 환율 흐름 배제 — 클라이언트에서 통화를 바꿔도 KRW 고정
        // if (request.getPaymentCurrency() != null) {
        //     draft.setPaymentCurrency(request.getPaymentCurrency());
        // }
        draft.setPaymentCurrency(PAYMENT_CURRENCY_KRW);
        draft.setPaymentCurrencyRate(KRW_PAYMENT_CURRENCY_RATE);

        if (request.getUsedPointAmount() != null) {
            BigDecimal points = BigDecimal.valueOf(request.getUsedPointAmount()).max(BigDecimal.ZERO);
            if (identity.isUser()) {
                long uid = userService.findUserIdByPublicId(identity.getUserPublicId())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "USER_NOT_FOUND"));
                var user = userService.getUserById(uid);
                if (user == null || user.getPoint() == null || BigDecimal.valueOf(user.getPoint()).compareTo(points) < 0) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "INSUFFICIENT_POINTS");
                }
            } else {
                if (points.compareTo(BigDecimal.ZERO) > 0) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "GUEST_CANNOT_USE_POINTS");
                }
            }
            draft.setUsedPointAmount(points.setScale(0, RoundingMode.HALF_UP));
        }

        draft.setTotalProductAmount(draft.getSubtotalAmount());
        BigDecimal deliveryFee = computeDeliveryFee(draft.getTotalProductAmount(), draft.getDeliveryMethod());
        draft.setDeliveryFee(deliveryFee);
        BigDecimal total = draft.getTotalProductAmount().add(deliveryFee).subtract(draft.getUsedPointAmount()).setScale(0,
                RoundingMode.HALF_UP);
        if (total.compareTo(BigDecimal.ZERO) < 0) {
            total = BigDecimal.ZERO;
        }
        draft.setTotalAmount(total);
        syncSettleKrwAmount(draft);
        checkoutDraftRepository.save(draft);
        return toResponse(draft);
    }

    //정산 원화 금액 동기화 로직
    private void syncSettleKrwAmount(CheckoutDraft draft) {
        BigDecimal total = draft.getTotalAmount() != null ? draft.getTotalAmount() : BigDecimal.ZERO;
        // USD 환율 흐름 배제: 정산 금액 = KRW 결제 금액
        // BigDecimal rate = draft.getPaymentCurrencyRate() != null ? draft.getPaymentCurrencyRate() : BigDecimal.ONE;
        // BigDecimal krw = total.multiply(rate).setScale(0, RoundingMode.HALF_UP);
        draft.setSettleKrwAmount(total.setScale(0, RoundingMode.HALF_UP).longValue());
    }

    //드래프트 만료 시간 갱신 로직
    private CheckoutDraft refreshExpiryIfNeeded(CheckoutDraft draft) {
        if (draft.getStatus() == DraftStatus.READY && LocalDateTime.now().isAfter(draft.getExpiresAt())) {
            throw new ResponseStatusException(HttpStatus.GONE, "DRAFT_EXPIRED");
        }
        return draft;
    }

    //드래프트 만료 시간 갱신 로직 (수정 가능)
    private CheckoutDraft refreshExpiryIfNeededMutable(CheckoutDraft draft) {
        if (draft.getStatus() == DraftStatus.READY && LocalDateTime.now().isAfter(draft.getExpiresAt())) {
            draft.setStatus(DraftStatus.EXPIRED);
            checkoutDraftRepository.save(draft);
            throw new ResponseStatusException(HttpStatus.GONE, "DRAFT_EXPIRED");
        }
        return draft;
    }

    //드래프트 소유자 검증 로직
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

    //사용자 ID 조회 로직
    private Long resolveUserIdPk(UserIdentity identity) {
        if (!identity.isUser()) {
            return null;
        }
        return userService.findUserIdByPublicId(identity.getUserPublicId()).orElse(null);
    }

    //장바구니 로드 로직
    private Cart loadCartOrThrow(Long userIdPk, String guestId) {
        if (userIdPk != null) {
            return cartRepository.findByUserId(userIdPk)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "EMPTY_CART"));
        }
        return cartRepository.findByGuestId(guestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "EMPTY_CART"));
    }

    // 배송비 계산: 무료 기준은 상품 합계만 비교한다(배송비 미포함).
    private BigDecimal computeDeliveryFee(BigDecimal productSubtotal, DeliveryMethod method) {
        if (method == DeliveryMethod.STORE_PICKUP) {
            return BigDecimal.ZERO;
        }
        Optional<BigDecimal> freeShippingThreshold = parseConfigDecimal(CFG_FREE_SHIPPING_THRESHOLD);
        BigDecimal fee = resolveStandardDeliveryFee();
        if (freeShippingThreshold.isPresent()
                && productSubtotal.compareTo(freeShippingThreshold.get()) >= 0) {
            return BigDecimal.ZERO;
        }
        return fee;
    }

    /** 택배 배송 시 설정된 기준 배송비(무료배송 기준 미적용). 수령 방식과 무관하게 설정값을 반환한다. */
    private BigDecimal resolveStandardDeliveryFee() {
        return parseConfigDecimal(CFG_SHIPPING_FEE).orElse(DEFAULT_SHIPPING_FEE)
                .setScale(0, RoundingMode.HALF_UP);
    }

    // USD 환율 조회 로직 (게임별) — KRW 전용으로 배제
    // private BigDecimal resolveUsCurrencyRate(String game) {
    //     if (game == null || game.isBlank()) {
    //         return BigDecimal.ONE;
    //     }
    //     String configGame = GameEnum.toConfigGame(game);
    //     return priceConfigService.getPriceConfig(CFG_US_CURRENCY_RATE, configGame)
    //             .map(PriceConfigDto::getConfigValue)
    //             .filter(v -> v != null)
    //             .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
    //                     "CONFIG_NOT_FOUND: us_currency_rate for game=" + configGame));
    // }

    //주문 설정 조회 로직
    private Optional<BigDecimal> parseConfigDecimal(String key) {
        return orderConfigRepository.findByConfigKey(key)
                .filter(OrderConfig::getIsEnabled)
                .map(OrderConfig::getConfigValue)
                .filter(v -> v != null && !v.isBlank())
                .map(BigDecimal::new);
    }

    //두 문자열 중 비어있지 않은 첫 번째 문자열 반환 로직
    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) {
            return a;
        }
        if (b != null && !b.isBlank()) {
            return b;
        }
        return null;
    }

    //드래프트 응답 생성 로직
    private CheckoutDraftResponse toResponse(CheckoutDraft draft) {
        List<CheckoutDraftItemResponse> items = draft.getItems().stream()
                .sorted(Comparator.comparing(CheckoutDraftItem::getSearchMapId))
                .map(it -> CheckoutDraftItemResponse.builder()
                        .searchMapId(it.getSearchMapId())
                        .productType(it.getProductType())
                        .productNameEn(it.getProductNameEn())
                        .productNameKo(it.getProductNameKo())
                        .imageUrl(it.getImageUrl())
                        .imageUrlEn(it.getImageUrlEn())
                        .imageUrlKo(it.getImageUrlKo())
                        .snapshotUnitPrice(it.getSnapshotUnitPrice())
                        .quantity(it.getQuantity())
                        .snapshotTotalPrice(it.getSnapshotTotalPrice())
                        .snapshotPointAmount(it.getSnapshotPointAmount())
                        .build())
                .collect(Collectors.toList());

        return CheckoutDraftResponse.builder()
                .publicId(draft.getPublicId())
                .status(draft.getStatus())
                .deliveryMethod(draft.getDeliveryMethod())
                .recipientName(draft.getRecipientName())
                .recipientAddress(draft.getRecipientAddress())
                .recipientAddressDetail(draft.getRecipientAddressDetail())
                .recipientPostalCode(draft.getRecipientPostalCode())
                .recipientPhone(draft.getRecipientPhone())
                .recipientEmail(draft.getRecipientEmail())
                .orderRequest(draft.getOrderRequest())
                .subtotalAmount(draft.getSubtotalAmount())
                .deliveryFee(draft.getDeliveryFee())
                .standardDeliveryFee(resolveStandardDeliveryFee())
                .freeShippingThreshold(parseConfigDecimal(CFG_FREE_SHIPPING_THRESHOLD).orElse(null))
                .usedPointAmount(draft.getUsedPointAmount())
                .totalAmount(draft.getTotalAmount())
                .totalProductAmount(draft.getTotalProductAmount())
                .expiresAt(draft.getExpiresAt())
                .createdAt(draft.getCreatedAt())
                .confirmedOrderId(draft.getConfirmedOrderId())
                .items(items)
                .hasEventTicket(hasEventTicket(draft.getItems()))
                .build();
    }

    //이벤트 티켓 여부 조회 로직
    private Boolean hasEventTicket(List<CheckoutDraftItem> items) {
        List<Long> manualSourceIds = items.stream()
                .filter(it -> ProductTableEnum.MANUAL_PRODUCT.name().equals(it.getTableName()))
                .map(CheckoutDraftItem::getSourceId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (manualSourceIds.isEmpty()) {
            return false;
        }
        return manualProductRepository.findAllById(manualSourceIds).stream()
                .map(ManualProduct::getProductType)
                .anyMatch(CheckoutDraftServiceImpl::isEventTicketProductType);
    }

    //이벤트 티켓 제품 타입 여부 조회 로직
    private static boolean isEventTicketProductType(String productType) {
        if (productType == null || productType.isBlank()) {
            return false;
        }
        String normalized = productType.trim().replace('_', '-').toLowerCase(Locale.ROOT);
        return EVENT_TICKET_TYPE_NORMALIZED.equals(normalized);
    }

    // private static boolean isCardCatalogTable(ProductSearchMap map) {
    //     ProductTableEnum table = map.getTableName();
    //     return table == ProductTableEnum.UNION_PRICE || table == ProductTableEnum.CARD_PRODUCT;
    // }

    /**
     * {@link com.shop.checkout.lines.CheckoutLineHandlerRegistry}에 등록된 테이블과 동일한 정책:
     * 체크아웃 확정 가능한 라인만 드래프트에 담는다.
     */
    private static void assertCheckoutSupportedProductTable(ProductSearchMap map) {
        ProductTableEnum table = map.getTableName();
        if (table == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "PRODUCT_UNAVAILABLE");
        }
        if (table != ProductTableEnum.CARD_PRODUCT
                && table != ProductTableEnum.SEALED_PRODUCT
                && table != ProductTableEnum.MANUAL_PRODUCT
                && table != ProductTableEnum.SUPPLY) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "UNSUPPORTED_PRODUCT_TABLE");
        }
    }
}
