package com.shop.admin.order.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.shop.admin.order.dto.AdminOrderCardProductDto;
import com.shop.admin.order.dto.AdminOrderCardProductGroupDto;
import com.shop.admin.order.dto.AdminOrderDetailDto;
import com.shop.admin.order.dto.AdminOrderInfoDto;
import com.shop.admin.order.dto.AdminOrderManualProductDto;
import com.shop.admin.order.dto.AdminOrderProductModifyRequest;
import com.shop.admin.order.dto.AdminOrderAdjustmentResponse;
import com.shop.admin.order.dto.AdminOrderSealedProductDto;
import com.shop.admin.order.dto.AdminOrderSupplyProductDto;
import com.shop.admin.order.dto.AdminOrderSearchRequest;
import com.shop.admin.order.dto.AdminSimpleOrderRequest;
import com.shop.admin.order.dto.AdminTodayOrderSummaryDto;
import com.shop.admin.order.dto.AdminTotalOrderSummaryDto;
import com.shop.order.adjustment.OrderAdjustmentCommand;
import com.shop.order.adjustment.OrderAdjustmentOrchestrator;
import com.shop.order.adjustment.guard.OrderCancelledGuard;
import com.shop.card.entity.UnionPrice;
import com.shop.card.metadata.entity.FabSetInfo;
import com.shop.card.metadata.entity.MtgSetInfo;
import com.shop.card.metadata.repository.FabSetInfoRepository;
import com.shop.card.metadata.repository.MtgSetInfoRepository;
import com.shop.card.metadata.support.FabSetCode;
import com.shop.card.metadata.support.PrintingResolver;
import com.shop.card.repository.UnionPriceRepository;
import com.shop.log.point.dto.PointLogDto;
import com.shop.log.point.entity.PointLogActorType;
import com.shop.log.point.repository.PointLogRepository;
import com.shop.log.point.service.PointLogService;
import com.shop.order.dto.AdminOrderSimpleDto;
import com.shop.order.dto.OrderConfigDto;
import com.shop.order.dto.OrderInfoListCriteria;
import com.shop.order.dto.OrderInfoSearchCriteria;
import com.shop.order.entity.OrderConfig;
import com.shop.order.entity.OrderInfo;
import com.shop.order.entity.OrderProduct;
import com.shop.order.enums.OrderStatus;
import com.shop.order.point.OrderEarnedPointCalculator;
import com.shop.order.repository.OrderConfigRepository;
import com.shop.order.repository.OrderInfoRepository;
import com.shop.order.repository.OrderProductRepository;
import com.shop.order.support.OrderInfoPersonalDataCodec;
import com.shop.product.entity.card.CardProduct;
import com.shop.product.entity.sealedProduct.SealedProduct;
import com.shop.product.enums.GameEnum;
import com.shop.product.metadata.dto.TcgPSetInfoDto;
import com.shop.product.metadata.entity.Storage;
import com.shop.product.metadata.repository.StorageRepository;
import com.shop.product.metadata.service.TcgPSetNameService;
import com.shop.product.repository.card.CardProductRepository;
import com.shop.product.repository.manualProduct.ManualProductRepository;
import com.shop.product.repository.sealedProduct.SealedProductRepository;
import com.shop.product.repository.supply.SupplyRepository;
import com.shop.search.dto.enums.ProductTableEnum;
import com.shop.search.entity.ProductSearchMap;
import com.shop.search.repository.map.ProductSearchMapRepository;
import com.shop.search.service.ProductSearchMapStockSyncPublisher;
import com.shop.user.entity.User;
import com.shop.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminOrderServiceImpl implements AdminOrderService {

    private static final String ORDER_DETAIL_LOG = "[ORDER-DETAIL]";

    private final CardProductRepository cardProductRepository;

    private final ManualProductRepository manualProductRepository;

    private final SealedProductRepository sealedProductRepository;

    private final SupplyRepository supplyRepository;

    private final OrderConfigRepository orderConfigRepository;

    private final OrderInfoRepository orderInfoRepository;

    private final OrderProductRepository orderProductRepository;

    private final ProductSearchMapRepository productSearchMapRepository;

    private final MtgSetInfoRepository mtgSetInfoRepository;

    private final FabSetInfoRepository fabSetInfoRepository;

    private final UnionPriceRepository unionPriceRepository;

    private final StorageRepository storageRepository;

    private final TcgPSetNameService tcgPSetNameService;

    private final ProductSearchMapStockSyncPublisher productSearchMapStockSyncPublisher;

    private final UserRepository userRepository;

    private final PointLogService pointLogService;

    private final PointLogRepository pointLogRepository;

    private final OrderInfoPersonalDataCodec orderInfoPersonalDataCodec;

    private final OrderAdjustmentOrchestrator orderAdjustmentOrchestrator;

    private final OrderCancelledGuard orderCancelledGuard;

    /**
     * @param adminSimpleOrderRequest
     * @return Page<AdminOrderSimpleDto>
     *         주문 목록 조회
     */
    @Override
    public Page<AdminOrderSimpleDto> getAdminOrderSimpleList(AdminSimpleOrderRequest adminSimpleOrderRequest) {
        Objects.requireNonNull(adminSimpleOrderRequest.getPageParam(), "pageParam");
        OrderInfoListCriteria criteria = OrderInfoListCriteria.builder()
                .pageParam(adminSimpleOrderRequest.getPageParam())
                .startDate(adminSimpleOrderRequest.getStartDate())
                .endDate(adminSimpleOrderRequest.getEndDate())
                .orderStatusList(adminSimpleOrderRequest.getOrderStatusList())
                .build();
        return orderInfoRepository.findWithFilters(criteria).map(this::toAdminOrderSimpleDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AdminOrderSimpleDto> searchAdminOrders(AdminOrderSearchRequest request) {
        Objects.requireNonNull(request.getPageParam(), "pageParam");
        String keyword = request.getKeyword() == null ? "" : request.getKeyword().trim();
        if (keyword.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "KEYWORD_REQUIRED");
        }
        OrderInfoSearchCriteria criteria = OrderInfoSearchCriteria.builder()
                .pageParam(request.getPageParam())
                .keyword(keyword)
                .build();
        return orderInfoRepository.findByKeyword(criteria).map(this::toAdminOrderSimpleDto);
    }

    @Override
    @Transactional(readOnly = true)
    public AdminTodayOrderSummaryDto getTodayOrderSummary() {
        LocalDate today = LocalDate.now();
        LocalDateTime start = today.atStartOfDay();
        LocalDateTime end = today.atTime(LocalTime.MAX);

        List<OrderInfo> todayOrders = orderInfoRepository.findByEffectiveOrderAtBetween(start, end);

        long orderPendingCount = 0;
        long orderCompletedCount = 0;
        long orderReceivedCount = 0;
        long totalOrderAmount = 0;

        for (OrderInfo order : todayOrders) {
            String status = normalizeOrderStatus(order.getOrderStatus());
            if (OrderStatus.ORDER_PENDING.getValue().equals(status)) {
                orderPendingCount++;
            } else if (OrderStatus.ORDER_COMPLETED.getValue().equals(status)) {
                orderCompletedCount++;
            } else if (OrderStatus.ORDER_RECEIPT_COMPLETED.getValue().equals(status)) {
                orderReceivedCount++;
            }
            if (!OrderStatus.ORDER_CANCELLED.getValue().equals(status)) {
                totalOrderAmount += paymentAmountAsLong(order.getTotalPaymentAmount());
            }
        }

        return AdminTodayOrderSummaryDto.builder()
                .totalOrderCount(todayOrders.size())
                .orderPendingCount(orderPendingCount)
                .orderCompletedCount(orderCompletedCount)
                .orderReceivedCount(orderReceivedCount)
                .totalOrderAmount(totalOrderAmount)
                .build();
    }

    private static String normalizeOrderStatus(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.trim().toUpperCase();
    }

    private static long paymentAmountAsLong(BigDecimal amount) {
        return amount != null ? amount.longValue() : 0L;
    }

    /** 엔티티 → 관리자 목록 응답 (리포지토리는 엔티티만 반환하고, 표현은 서비스에서 결정). */
    private AdminOrderSimpleDto toAdminOrderSimpleDto(OrderInfo orderInfo) {
        return AdminOrderSimpleDto.builder()
                .id(orderInfo.getId())
                .orderDate(orderInfo.getEffectiveOrderAt())
                .orderStatus(orderInfo.getOrderStatus())
                .customerName(orderInfo.getRecipientName())
                .customerContact(orderInfoPersonalDataCodec.decrypt(orderInfo.getRecipientPhone()))
                .customerEmail(orderInfoPersonalDataCodec.decrypt(orderInfo.getRecipientEmail()))
                .deliveryCompany(orderInfo.getDeliveryCompany())
                .totalPaymentAmount(orderInfo.getTotalProductAmount().add(orderInfo.getDeliveryFee()))
                .totalQuantity(orderInfo.getTotalQuantity())
                .orderLineCount(orderInfo.getOrderLineCount())
                .usedPointAmount(orderInfo.getUsedPointAmount())
                .deliveryFee(orderInfo.getDeliveryFee())
                .actualPaymentAmount(orderInfo.getActualPaymentAmount())
                .paymentMethod(orderInfo.getPaymentMethod())
                .build();
    }

    @Override
    public AdminOrderDetailDto getAdminOrderDetail(Long id) {
        OrderInfo orderInfo = orderInfoRepository.findById(id).orElseThrow(() -> new IllegalStateException("주문 정보가 존재하지 않습니다."));
        List<OrderProduct> allOrderProducts = orderProductRepository.findByOrderInfoId(orderInfo.getId());
        log.info("{} getAdminOrderDetail orderId={} orderLineCount={} dbOrderProductRows={}",
                ORDER_DETAIL_LOG, id, orderInfo.getOrderLineCount(), allOrderProducts.size());
        for (OrderProduct line : allOrderProducts) {
            log.info(
                    "{} orderProduct row id={} productId={} productTable={} productType={} "
                            + "searchMapId={} productPublicId={} nameEn={}",
                    ORDER_DETAIL_LOG,
                    line.getId(),
                    line.getProductId(),
                    line.getProductTable(),
                    line.getProductType(),
                    line.getSearchMapId(),
                    line.getProductPublicId(),
                    line.getProductNameEn());
        }
        Long totalEarnedPoints = orderInfo.getEarnedPointAmount();
        List<AdminOrderCardProductGroupDto> cardGroups = buildOrderCardProductGroups(orderInfo.getId());
        List<AdminOrderManualProductDto> manualProducts = allOrderProducts.stream()
                .filter(op -> "ManualProducts".equals(op.getProductType()))
                .map(this::toAdminOrderManualProductDto)
                .collect(Collectors.toList());
        List<AdminOrderSealedProductDto> sealedProducts = allOrderProducts.stream()
                .filter(op -> "SealedProducts".equals(op.getProductType()))
                .map(this::toAdminOrderSealedProductDto)
                .collect(Collectors.toList());
        List<AdminOrderSupplyProductDto> supplyProducts = allOrderProducts.stream()
                .filter(op -> "Supplies".equals(op.getProductType())
                        || op.getProductTable() == ProductTableEnum.SUPPLY)
                .map(this::toAdminOrderSupplyProductDto)
                .collect(Collectors.toList());
        int cardLineCount = cardGroups.stream()
                .mapToInt(g -> g.getProducts() == null ? 0 : g.getProducts().size())
                .sum();
        log.info(
                "{} getAdminOrderDetail built orderId={} cardGroups={} cardLines={} manualLines={} sealedLines={} supplyLines={}",
                ORDER_DETAIL_LOG,
                id,
                cardGroups.size(),
                cardLineCount,
                manualProducts.size(),
                sealedProducts.size(),
                supplyProducts.size());
        String userMemo = null;
        if (orderInfo.getUserId() != null) {
            User user = userRepository.findById(orderInfo.getUserId()).orElse(null);
            userMemo = user != null ? user.getUserMemo() : "오류: 탈퇴 회원";
        }
        return AdminOrderDetailDto.builder()
                .orderInfo(AdminOrderInfoDto.builder()
                .id(orderInfo.getId())
                .guest(orderInfo.getGuest())
                .userId(orderInfo.getUserId())
                .recipientName(orderInfo.getRecipientName())
                .recipientAddress(orderInfoPersonalDataCodec.decrypt(orderInfo.getRecipientAddress()))
                .recipientAddressDetail(orderInfoPersonalDataCodec.decrypt(orderInfo.getRecipientAddressDetail()))
                .recipientPhone(orderInfoPersonalDataCodec.decrypt(orderInfo.getRecipientPhone()))
                .recipientEmail(orderInfoPersonalDataCodec.decrypt(orderInfo.getRecipientEmail()))
                .postalCode(orderInfo.getPostalCode())
                .orderRequest(orderInfo.getOrderRequest())
                .orderStatus(orderInfo.getOrderStatus())
                .paymentStatus(orderInfo.getPaymentStatus())
                .deliveryCompany(orderInfo.getDeliveryCompany())
                .deliveryTrackingNumber(orderInfo.getDeliveryTrackingNumber())
                .deliveryMemo(orderInfo.getDeliveryMemo())
                .paymentCurrency(orderInfo.getPaymentCurrency())
                .totalProductAmount(orderInfo.getTotalProductAmount())
                .totalPaymentAmount(orderInfo.getTotalPaymentAmount())
                .usedPointAmount(orderInfo.getUsedPointAmount())
                .actualPaymentAmount(orderInfo.getActualPaymentAmount())
                .deliveryFee(orderInfo.getDeliveryFee())
                .totalQuantity(orderInfo.getTotalQuantity())
                .orderLineCount(orderInfo.getOrderLineCount())
                .paymentDate(orderInfo.getPaymentDate())
                .paymentMethod(orderInfo.getPaymentMethod())
                .pgTransactionId(orderInfo.getPgTransactionId())
                .paymentCurrencyRateSnapshot(orderInfo.getPaymentCurrencyRateSnapshot())
                .totalEarnedPoints(totalEarnedPoints)
                .userMemo(userMemo)
                    .build())
                .orderCardProductGroups(cardGroups)
                .orderManualProducts(manualProducts)
                .orderSealedProducts(sealedProducts)
                .orderSupplyProducts(supplyProducts)
                .build();
    }

    @Override
    @Transactional
    public void updateOrderStatus(Long id, String orderStatus) {
        if (OrderStatus.ORDER_CANCELLED.getValue().equals(orderStatus)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "USE_CANCEL_ENDPOINT");
        }

        OrderInfo orderInfo = orderInfoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "ORDER_NOT_FOUND"));

        orderCancelledGuard.assertNotCancelled(orderInfo);

        OrderStatus status = OrderStatus.fromValue(orderStatus)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "INVALID_ORDER_STATUS"));

        orderInfo.setOrderStatus(status.getValue());
        orderInfoRepository.save(orderInfo);

        if (OrderStatus.ORDER_DELIVERY_COMPLETED == status) {
            earnPointsOnDeliveryCompleted(orderInfo);
        }
    }

    private void earnPointsOnDeliveryCompleted(OrderInfo orderInfo) {
        if (orderInfo.getUserId() == null) {
            return;
        }

        long earnedPts = orderInfo.getEarnedPointAmount() != null ? orderInfo.getEarnedPointAmount() : 0L;
        if (earnedPts <= 0) {
            return;
        }

        String changeReason = OrderEarnedPointCalculator.earnChangeReason(orderInfo.getId());
        if (pointLogRepository.existsByChangeReason(changeReason)) {
            return;
        }

        User user = userRepository.findById(orderInfo.getUserId()).orElse(null);
        if (user == null) {
            return;
        }

        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        long beforePoint = user.getPoint() != null ? user.getPoint() : 0L;
        userRepository.addPoints(user.getId(), earnedPts);
        pointLogService.savePointLog(PointLogDto.builder()
                .userId(user.getId())
                .userName(user.getName())
                .changedPoint(earnedPts)
                .beforePoint(beforePoint)
                .afterPoint(beforePoint + earnedPts)
                .changeReason(changeReason)
                .actorType(PointLogActorType.SYSTEM)
                .actorDetail(null)
                .actionDate(now)
                .isSuccess(true)
                .build());
    }

    @Override
    @Transactional
    public void updateDeliveryTrackingNumber(Long id, String deliveryTrackingNumber) {
        updateDeliveryInfo(id, deliveryTrackingNumber, null, true, false);
    }

    @Override
    @Transactional
    public void updateDeliveryMemo(Long id, String deliveryMemo) {
        updateDeliveryInfo(id, null, deliveryMemo, false, true);
    }

    @Override
    @Transactional
    public void updateDeliveryInfo(Long id, String deliveryTrackingNumber, String deliveryMemo) {
        updateDeliveryInfo(id, deliveryTrackingNumber, deliveryMemo, true, true);
    }

    private void updateDeliveryInfo(
            Long id,
            String deliveryTrackingNumber,
            String deliveryMemo,
            boolean updateTracking,
            boolean updateMemo) {
        OrderInfo orderInfo = orderInfoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "ORDER_NOT_FOUND"));
        orderCancelledGuard.assertNotCancelled(orderInfo);
        if (updateTracking) {
            orderInfo.setDeliveryTrackingNumber(deliveryTrackingNumber);
        }
        if (updateMemo) {
            orderInfo.setDeliveryMemo(deliveryMemo);
        }
        orderInfoRepository.save(orderInfo);
    }

    @Override
    @Transactional
    public AdminOrderAdjustmentResponse cancelOrder(
            Long id, boolean restoreStock, boolean restoreCart, String cancelReason) {
        return orderAdjustmentOrchestrator.executeFullCancel(
                OrderAdjustmentCommand.fullCancel(id, restoreStock, restoreCart, cancelReason));
    }

    /** 그룹 헤더 표시 순서 (각 그룹 내 라인 정렬은 게임별 로직에서 처리). */
    private static final List<GameEnum> CARD_GROUP_ORDER = List.of(
            GameEnum.MTG, GameEnum.LORC, GameEnum.SWU, GameEnum.FAB, GameEnum.RIFT);

    private List<AdminOrderCardProductGroupDto> buildOrderCardProductGroups(Long orderInfoId) {
        List<OrderProduct> allLines = orderProductRepository.findByOrderInfoId(orderInfoId);
        List<OrderProduct> orderProducts = orderProductRepository.findByOrderInfoIdAndProductType(orderInfoId, "Cards");
        long cardTableLines = allLines.stream()
                .filter(op -> op.getProductTable() == ProductTableEnum.CARD_PRODUCT)
                .count();
        log.info(
                "{} buildOrderCardProductGroups orderInfoId={} productTypeCards={} productTableCardProduct={} allLines={}",
                ORDER_DETAIL_LOG,
                orderInfoId,
                orderProducts.size(),
                cardTableLines,
                allLines.size());
        if (orderProducts.isEmpty() && cardTableLines > 0) {
            log.warn(
                    "{} productType filter mismatch orderInfoId={}: {} row(s) have productTable=CARD_PRODUCT but productType!='Cards'",
                    ORDER_DETAIL_LOG,
                    orderInfoId,
                    cardTableLines);
            for (OrderProduct line : allLines) {
                if (line.getProductTable() == ProductTableEnum.CARD_PRODUCT) {
                    log.warn(
                            "{} CARD_PRODUCT row id={} productType='{}' searchMapId={} productPublicId={}",
                            ORDER_DETAIL_LOG,
                            line.getId(),
                            line.getProductType(),
                            line.getSearchMapId(),
                            line.getProductPublicId());
                }
            }
        }

        Map<Long, ProductSearchMap> searchMapCache = new HashMap<>();
        List<OrderProduct> mtgCards = new ArrayList<>();
        List<OrderProduct> swuCards = new ArrayList<>();
        List<OrderProduct> lorcCards = new ArrayList<>();
        List<OrderProduct> fabCards = new ArrayList<>();
        List<OrderProduct> riftCards = new ArrayList<>();
        int unclassifiedCards = 0;
        for (OrderProduct orderProduct : orderProducts) {
            log.info(
                    "{} card line before searchMap lookup orderProductId={} searchMapId={} productPublicId={}",
                    ORDER_DETAIL_LOG,
                    orderProduct.getId(),
                    orderProduct.getSearchMapId(),
                    orderProduct.getProductPublicId());
            ProductSearchMap psm = productSearchMapRepository.findById(orderProduct.getSearchMapId())
                    .orElseThrow(() -> {
                        log.error(
                                "{} searchMap not found orderInfoId={} orderProductId={} searchMapId={} productPublicId={}",
                                ORDER_DETAIL_LOG,
                                orderInfoId,
                                orderProduct.getId(),
                                orderProduct.getSearchMapId(),
                                orderProduct.getProductPublicId());
                        return new IllegalStateException("검색 매핑이 존재하지 않습니다.");
                    });
            searchMapCache.put(orderProduct.getSearchMapId(), psm);
            log.info(
                    "{} searchMap resolved searchMapId={} tableName={} game={} catalogSourceId={} sourceId={} productId={}",
                    ORDER_DETAIL_LOG,
                    psm.getId(),
                    psm.getTableName(),
                    psm.getGame(),
                    psm.getCatalogSourceId(),
                    psm.getSourceId(),
                    psm.getProductId());

            String game = psm.getGame();
            if (game == null) {
                unclassifiedCards++;
                log.warn("{} searchMap game is null searchMapId={} orderProductId={}",
                        ORDER_DETAIL_LOG, psm.getId(), orderProduct.getId());
                continue;
            }
            Optional<GameEnum> matchedGame = GameEnum.fromName(game);
            if (matchedGame.isEmpty()) {
                unclassifiedCards++;
                log.warn(
                        "{} card game not handled orderProductId={} game='{}' searchMapId={}",
                        ORDER_DETAIL_LOG,
                        orderProduct.getId(),
                        game,
                        psm.getId());
                continue;
            }
            switch (matchedGame.get()) {
                case MTG -> mtgCards.add(orderProduct);
                case SWU -> swuCards.add(orderProduct);
                case LORC -> lorcCards.add(orderProduct);
                case FAB -> fabCards.add(orderProduct);
                case RIFT -> riftCards.add(orderProduct);
                default -> {
                    unclassifiedCards++;
                    log.warn(
                            "{} card game not handled orderProductId={} game='{}' searchMapId={}",
                            ORDER_DETAIL_LOG,
                            orderProduct.getId(),
                            game,
                            psm.getId());
                }
            }
        }
        log.info(
                "{} card classification orderInfoId={} mtg={} swu={} lorc={} fab={} rift={} unclassified={}",
                ORDER_DETAIL_LOG,
                orderInfoId,
                mtgCards.size(),
                swuCards.size(),
                lorcCards.size(),
                fabCards.size(),
                riftCards.size(),
                unclassifiedCards);

        Map<String, List<AdminOrderCardProductDto>> byGameKey = new LinkedHashMap<>();

        // 3. MTG 카드: MtgSetInfo(releaseDate) + UnionPrice(setNumber) 연동-클라이언트 요청으로 set abc 순으로 변경
        List<AdminOrderCardProductDto> mtgLines = new ArrayList<>();
        for (OrderProduct op : mtgCards) {
            ProductSearchMap psm = searchMapCache.get(op.getSearchMapId());

            MtgSetInfo setInfo = mtgSetInfoRepository.findBySetCode(psm.getSetCode())
                    .orElseThrow(() -> new IllegalStateException("세트 정보가 존재하지 않습니다: " + psm.getSetCode()));
            UnionPrice unionPrice = unionPriceRepository.findById(psm.getCatalogSourceId())
                    .orElseThrow(() -> new IllegalStateException("UnionPrice가 존재하지 않습니다: " + psm.getSourceId()));

            CardProduct cardProduct = cardProductRepository.findByPublicId(psm.getProductId())
                    .orElseThrow(() -> new IllegalStateException("카드 상품이 존재하지 않습니다: " + psm.getProductId()));

            AdminOrderCardProductDto dto = toAdminOrderCardProductDto(op);
            dto.setProductNameEn(op.getProductNameEn());
            dto.setProductNameKo(op.getProductNameKo());
            dto.setGame(GameEnum.MTG.getGame());
            dto.setSetCode(setInfo.getSetCode());
            dto.setSetName(setInfo.getName());
            dto.setReleaseDate(setInfo.getReleaseDate());
            dto.setSetNumber(unionPrice.getSetNumber());
            dto.setStorageName(resolveStorageNameBySearchMap(psm));
            dto.setPrintType(unionPrice.getPrintType());
            dto.setPrinting(PrintingResolver.resolve(unionPrice));
            dto.setLanguage(cardProduct.getLanguage());
            dto.setCondition(cardProduct.getCondition());
            dto.setTotalStock(cardProduct.getTotalStock());
            dto.setMemo(cardProduct.getMemo());
            mtgLines.add(dto);
        }

        mtgLines.sort(Comparator
                .comparing(AdminOrderCardProductDto::getSetCode,
                        Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparingLong(dto -> dto.getSetNumber() != null ? dto.getSetNumber() : Long.MAX_VALUE));
        if (!mtgLines.isEmpty()) {
            byGameKey.put(GameEnum.MTG.getGame(), mtgLines);
        }

        List<AdminOrderCardProductDto> fabLines = new ArrayList<>();
        Map<String, String> fabSetNameByDisplaySetCode = new HashMap<>();
        for (OrderProduct op : fabCards) {
            ProductSearchMap psm = searchMapCache.get(op.getSearchMapId());

            FabSetInfo setInfo = resolveFabSetInfo(psm.getSetCode());
            String displaySetCode = FabSetCode.toDisplayCode(setInfo.getSetCode());
            fabSetNameByDisplaySetCode.put(displaySetCode, setInfo.getName());
            UnionPrice unionPrice = unionPriceRepository.findById(psm.getCatalogSourceId())
                    .orElseThrow(() -> new IllegalStateException("UnionPrice가 존재하지 않습니다: " + psm.getSourceId()));

            CardProduct cardProduct = cardProductRepository.findByPublicId(psm.getProductId())
                    .orElseThrow(() -> new IllegalStateException("카드 상품이 존재하지 않습니다: " + psm.getProductId()));

            AdminOrderCardProductDto dto = toAdminOrderCardProductDto(op);
            dto.setProductNameEn(op.getProductNameEn());
            dto.setProductNameKo(op.getProductNameKo());
            dto.setGame(GameEnum.FAB.getGame());
            dto.setSetCode(displaySetCode);
            dto.setSetName(setInfo.getName());
            dto.setSetNumber(unionPrice.getSetNumber());
            dto.setStorageName(resolveStorageNameBySearchMap(psm));
            dto.setPrintType(unionPrice.getPrintType());
            dto.setPrinting(PrintingResolver.resolve(unionPrice));
            dto.setLanguage(cardProduct.getLanguage());
            dto.setCondition(cardProduct.getCondition());
            dto.setTotalStock(cardProduct.getTotalStock());
            dto.setMemo(cardProduct.getMemo());
            fabLines.add(dto);
        }

        sortCardLinesBySetNameAsc(fabLines, fabSetNameByDisplaySetCode);
        if (!fabLines.isEmpty()) {
            byGameKey.put(GameEnum.FAB.getGame(), fabLines);
        }

        List<AdminOrderCardProductDto> swuLines = new ArrayList<>();
        Map<String, String> swuSetNameBySetCode = new HashMap<>();
        for (OrderProduct op : swuCards) {
            ProductSearchMap psm = searchMapCache.get(op.getSearchMapId());

            TcgPSetInfoDto setInfo = tcgPSetNameService.findSetInfoByCategoryIdAndSetName(GameEnum.SWU.getProductId(), psm.getSetCode())
                    .orElseThrow(() -> new IllegalStateException("세트 정보가 존재하지 않습니다: " + psm.getSetCode()));
            swuSetNameBySetCode.put(setInfo.getSetCode(), setInfo.getSetName());
            UnionPrice unionPrice = unionPriceRepository.findById(psm.getCatalogSourceId())
                    .orElseThrow(() -> new IllegalStateException("UnionPrice가 존재하지 않습니다: " + psm.getSourceId()));

            CardProduct cardProduct = cardProductRepository.findByPublicId(psm.getProductId())
                    .orElseThrow(() -> new IllegalStateException("카드 상품이 존재하지 않습니다: " + psm.getProductId()));

            AdminOrderCardProductDto dto = toAdminOrderCardProductDto(op);
            dto.setProductNameEn(op.getProductNameEn());
            dto.setProductNameKo(op.getProductNameKo());
            dto.setGame(GameEnum.SWU.getGame());
            dto.setSetCode(setInfo.getSetCode());
            dto.setSetName(setInfo.getSetName());
            dto.setReleaseDate(setInfo.getReleaseDate());
            dto.setSetNumber(unionPrice.getSetNumber());
            dto.setStorageName(resolveStorageNameBySearchMap(psm));
            dto.setPrintType(unionPrice.getPrintType());
            dto.setPrinting(PrintingResolver.resolve(unionPrice));
            dto.setLanguage(cardProduct.getLanguage());
            dto.setCondition(cardProduct.getCondition());
            dto.setTotalStock(cardProduct.getTotalStock());
            dto.setMemo(cardProduct.getMemo());
            swuLines.add(dto);
        }

        sortCardLinesBySetNameAsc(swuLines, swuSetNameBySetCode);
        if (!swuLines.isEmpty()) {
            byGameKey.put(GameEnum.SWU.getGame(), swuLines);
        }
        
        List<AdminOrderCardProductDto> lorcLines = new ArrayList<>();
        Map<String, String> lorcSetNameBySetCode = new HashMap<>();
        for (OrderProduct op : lorcCards) {
            ProductSearchMap psm = searchMapCache.get(op.getSearchMapId());

            TcgPSetInfoDto setInfo = tcgPSetNameService.findSetInfoByCategoryIdAndSetName(GameEnum.LORC.getProductId(), psm.getSetCode())
                    .orElseThrow(() -> new IllegalStateException("세트 정보가 존재하지 않습니다: " + psm.getSetCode()));
            lorcSetNameBySetCode.put(setInfo.getSetCode(), setInfo.getSetName());
            UnionPrice unionPrice = unionPriceRepository.findById(psm.getCatalogSourceId())
                    .orElseThrow(() -> new IllegalStateException("UnionPrice가 존재하지 않습니다: " + psm.getSourceId()));

            CardProduct cardProduct = cardProductRepository.findByPublicId(psm.getProductId())
                    .orElseThrow(() -> new IllegalStateException("카드 상품이 존재하지 않습니다: " + psm.getProductId()));

            AdminOrderCardProductDto dto = toAdminOrderCardProductDto(op);
            dto.setProductNameEn(op.getProductNameEn());
            dto.setProductNameKo(op.getProductNameKo());
            dto.setGame(GameEnum.LORC.getGame());
            dto.setSetCode(setInfo.getSetCode());
            dto.setSetName(setInfo.getSetName());
            dto.setReleaseDate(setInfo.getReleaseDate());
            dto.setSetNumber(unionPrice.getSetNumber());
            dto.setStorageName(resolveStorageNameBySearchMap(psm));
            dto.setPrintType(unionPrice.getPrintType());
            dto.setPrinting(PrintingResolver.resolve(unionPrice));
            dto.setLanguage(cardProduct.getLanguage());
            dto.setCondition(cardProduct.getCondition());
            dto.setTotalStock(cardProduct.getTotalStock());
            dto.setMemo(cardProduct.getMemo());
            lorcLines.add(dto);
        }

        sortCardLinesBySetNameAsc(lorcLines, lorcSetNameBySetCode);
        if (!lorcLines.isEmpty()) {
            byGameKey.put(GameEnum.LORC.getGame(), lorcLines);
        }

        List<AdminOrderCardProductDto> riftLines = new ArrayList<>();
        Map<String, String> riftSetNameBySetCode = new HashMap<>();
        for (OrderProduct op : riftCards) {
            ProductSearchMap psm = searchMapCache.get(op.getSearchMapId());

            TcgPSetInfoDto setInfo = tcgPSetNameService.findSetInfoByCategoryIdAndSetName(GameEnum.RIFT.getProductId(), psm.getSetCode())
                    .orElseThrow(() -> new IllegalStateException("세트 정보가 존재하지 않습니다: " + psm.getSetCode()));
            riftSetNameBySetCode.put(setInfo.getSetCode(), setInfo.getSetName());
            UnionPrice unionPrice = unionPriceRepository.findById(psm.getCatalogSourceId())
                    .orElseThrow(() -> new IllegalStateException("UnionPrice가 존재하지 않습니다: " + psm.getSourceId()));

            CardProduct cardProduct = cardProductRepository.findByPublicId(psm.getProductId())
                    .orElseThrow(() -> new IllegalStateException("카드 상품이 존재하지 않습니다: " + psm.getProductId()));

            AdminOrderCardProductDto dto = toAdminOrderCardProductDto(op);
            dto.setProductNameEn(op.getProductNameEn());
            dto.setProductNameKo(op.getProductNameKo());
            dto.setGame(GameEnum.RIFT.getGame());
            dto.setSetCode(setInfo.getSetCode());
            dto.setSetName(setInfo.getSetName());
            dto.setReleaseDate(setInfo.getReleaseDate());
            dto.setSetNumber(unionPrice.getSetNumber());
            dto.setStorageName(resolveStorageNameBySearchMap(psm));
            dto.setPrintType(unionPrice.getPrintType());
            dto.setPrinting(PrintingResolver.resolve(unionPrice));
            dto.setLanguage(cardProduct.getLanguage());
            dto.setCondition(cardProduct.getCondition());
            dto.setTotalStock(cardProduct.getTotalStock());
            dto.setMemo(cardProduct.getMemo());
            riftLines.add(dto);
        }

        sortCardLinesBySetNameAsc(riftLines, riftSetNameBySetCode);
        if (!riftLines.isEmpty()) {
            byGameKey.put(GameEnum.RIFT.getGame(), riftLines);
        }

        List<AdminOrderCardProductGroupDto> groups = new ArrayList<>();
        for (GameEnum g : CARD_GROUP_ORDER) {
            List<AdminOrderCardProductDto> lines = byGameKey.remove(g.getGame());
            if (lines != null && !lines.isEmpty()) {
                groups.add(AdminOrderCardProductGroupDto.builder()
                        .game(g.getGame())
                        .products(lines)
                        .build());
            }
        }
        for (Map.Entry<String, List<AdminOrderCardProductDto>> e : byGameKey.entrySet()) {
            if (e.getValue() != null && !e.getValue().isEmpty()) {
                groups.add(AdminOrderCardProductGroupDto.builder()
                        .game(e.getKey())
                        .products(e.getValue())
                        .build());
            }
        }
        log.info(
                "{} buildOrderCardProductGroups done orderInfoId={} groups={} totalLines={}",
                ORDER_DETAIL_LOG,
                orderInfoId,
                groups.size(),
                groups.stream().mapToInt(g -> g.getProducts() == null ? 0 : g.getProducts().size()).sum());
        for (AdminOrderCardProductGroupDto group : groups) {
            log.info("{} card group game='{}' lines={}",
                    ORDER_DETAIL_LOG,
                    group.getGame(),
                    group.getProducts() == null ? 0 : group.getProducts().size());
        }
        return groups;
    }

    /** FAB·TCG(SWU/LORC/RIFT) 공통: 세트 name 오름차순 → setNumber 오름차순 */
    private static void sortCardLinesBySetNameAsc(
            List<AdminOrderCardProductDto> lines,
            Map<String, String> setNameBySetCode) {
        lines.sort(Comparator
                .comparing(
                        (AdminOrderCardProductDto dto) -> setNameBySetCode.getOrDefault(dto.getSetCode(), ""),
                        Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                .thenComparingLong(dto -> dto.getSetNumber() != null ? dto.getSetNumber() : Long.MAX_VALUE));
    }

    private FabSetInfo resolveFabSetInfo(String setCode) {
        return fabSetInfoRepository
                .findBySetCodeIn(List.of(setCode, FabSetCode.toSourceCode(setCode), FabSetCode.toDisplayCode(setCode)))
                .stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("세트 정보가 존재하지 않습니다: " + setCode));
    }

    /** {@link ProductSearchMap#getProductId()}는 {@link CardProduct#getPublicId()}와 동일. */
    private String resolveStorageNameBySearchMap(ProductSearchMap psm) {
        CardProduct cardProduct = cardProductRepository.findByPublicId(psm.getProductId())
                .orElseThrow(() -> new IllegalStateException("카드 상품이 존재하지 않습니다: " + psm.getProductId()));
        Long storageId = cardProduct.getStorageId();
        if (storageId == null) {
            return null;
        }
        return storageRepository.findById(storageId)
                .map(Storage::getStorageName)
                .orElseThrow(() -> new IllegalStateException("저장소가 존재하지 않습니다: " + storageId));
    }

    private AdminOrderSealedProductDto toAdminOrderSealedProductDto(OrderProduct orderProduct) {
        AdminOrderSealedProductDto dto = new AdminOrderSealedProductDto();
        dto.setId(orderProduct.getId());
        dto.setOrderInfoId(orderProduct.getOrderInfoId());
        dto.setProductId(orderProduct.getProductId());
        dto.setQuantity(orderProduct.getQuantity());
        dto.setPrice(orderProduct.getPrice());
        dto.setTotalPrice(orderProduct.getTotalPrice());
        dto.setImageUrl(orderProduct.getImageUrl());
        dto.setProductNameEn(orderProduct.getProductNameEn());
        dto.setProductNameKo(orderProduct.getProductNameKo());
        SealedProduct sealed = sealedProductRepository.findById(orderProduct.getProductId())
                .orElseThrow(() -> new IllegalStateException(
                        "SealedProduct가 존재하지 않습니다: " + orderProduct.getProductId()));
        dto.setGame(sealed.getGame());
        dto.setLanguage(sealed.getLanguage());
        Integer totalStock = sealed.getTotalStock();
        dto.setTotalStock(totalStock != null ? totalStock.longValue() : null);
        return dto;
    }
    private AdminOrderManualProductDto toAdminOrderManualProductDto(OrderProduct orderProduct) {
        AdminOrderManualProductDto dto = new AdminOrderManualProductDto();
        dto.setId(orderProduct.getId());
        dto.setOrderInfoId(orderProduct.getOrderInfoId());
        dto.setProductId(orderProduct.getProductId());
        dto.setQuantity(orderProduct.getQuantity());
        dto.setPrice(orderProduct.getPrice());
        dto.setTotalPrice(orderProduct.getTotalPrice());
        dto.setImageUrl(orderProduct.getImageUrl());
        dto.setProductNameEn(orderProduct.getProductNameEn());
        dto.setProductNameKo(orderProduct.getProductNameKo());
        return dto;
    }

    private AdminOrderSupplyProductDto toAdminOrderSupplyProductDto(OrderProduct orderProduct) {
        AdminOrderSupplyProductDto dto = new AdminOrderSupplyProductDto();
        dto.setId(orderProduct.getId());
        dto.setOrderInfoId(orderProduct.getOrderInfoId());
        dto.setProductId(orderProduct.getProductId());
        dto.setQuantity(orderProduct.getQuantity());
        dto.setPrice(orderProduct.getPrice());
        dto.setTotalPrice(orderProduct.getTotalPrice());
        dto.setImageUrl(orderProduct.getImageUrl());
        dto.setProductNameEn(orderProduct.getProductNameEn());
        dto.setProductNameKo(orderProduct.getProductNameKo());
        supplyRepository.findById(orderProduct.getProductId()).ifPresent(supply -> {
            dto.setSupplyType(supply.getSupplyType());
            dto.setMaker(supply.getMaker());
            dto.setTotalStock(supply.getStock());
        });
        return dto;
    }

    private AdminOrderCardProductDto toAdminOrderCardProductDto(OrderProduct orderProduct) {
        AdminOrderCardProductDto dto = new AdminOrderCardProductDto();
        dto.setId(orderProduct.getId());
        dto.setOrderInfoId(orderProduct.getOrderInfoId());
        dto.setProductId(orderProduct.getProductId());
        dto.setQuantity(orderProduct.getQuantity());
        dto.setPrice(orderProduct.getPrice());
        dto.setTotalPrice(orderProduct.getTotalPrice());
        dto.setImageUrl(orderProduct.getImageUrl());
        dto.setRewardPoints(orderProduct.getRewardPoints());
        return dto;
    }

    //===============================================
    // 주문 설정 관리
    //===============================================

    @Override
    public List<OrderConfigDto> getConfigList() {
        return orderConfigRepository.findAll().stream().map(orderConfig -> OrderConfigDto.builder()
        .configKey(orderConfig.getConfigKey())
        .configValue(orderConfig.getConfigValue())
        .isEnabled(orderConfig.getIsEnabled())
        .build()).collect(Collectors.toList());
    }

    @Override
    public void setShippingFee(OrderConfigDto orderConfigDto) {
        OrderConfig orderConfig = orderConfigRepository.findByConfigKey(orderConfigDto.getConfigKey()).orElse(null);
        if (orderConfig == null) {
            throw new IllegalStateException("배송비 설정이 존재하지 않습니다.");
        }
        orderConfig.setConfigValue(orderConfigDto.getConfigValue());
        orderConfig.setIsEnabled(orderConfigDto.getIsEnabled());
        orderConfigRepository.save(orderConfig);
    }

    @Override
    public void setFreeShippingThreshold(OrderConfigDto orderConfigDto) {
        OrderConfig orderConfig = orderConfigRepository.findByConfigKey(orderConfigDto.getConfigKey()).orElse(null);
        if (orderConfig == null) {
            throw new IllegalStateException("배송비 무료 기준 금액 설정이 존재하지 않습니다.");
        }
        orderConfig.setConfigValue(orderConfigDto.getConfigValue());
        orderConfig.setIsEnabled(orderConfigDto.getIsEnabled());
        orderConfigRepository.save(orderConfig);
    }

    @Override
    public AdminTotalOrderSummaryDto getTotalOrderSummary() {
        return AdminTotalOrderSummaryDto.builder()
                .orderPendingCount(orderInfoRepository.countByOrderStatus(OrderStatus.ORDER_PENDING.getValue()))
                .orderCompletedCount(orderInfoRepository.countByOrderStatus(OrderStatus.ORDER_COMPLETED.getValue()))
                .orderReceivedCount(orderInfoRepository.countByOrderStatus(OrderStatus.ORDER_RECEIPT_COMPLETED.getValue()))
                .build();
    }

    //===============================================
    // 부분 주문 수정
    //===============================================

    @Override
    @Transactional
    public AdminOrderAdjustmentResponse modifyOrderProducts(Long orderId, AdminOrderProductModifyRequest request) {
        return orderAdjustmentOrchestrator.executePartialModify(
                OrderAdjustmentCommand.partialModify(orderId, request));
    }
}
