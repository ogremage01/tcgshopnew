package com.shop.offline.product.service;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.shop.offline.product.dto.OfflineShippingReconcileResultDto;
import com.shop.offline.product.dto.OfflineStockSyncResultDto;
import com.shop.offline.product.entity.OfflineProduct;
import com.shop.offline.product.repository.OfflineProductRepository;
import com.shop.offline.sales.dto.projection.TitleQuantityProjection;
import com.shop.offline.sales.repository.OfflineSalesItemRepository;
import com.shop.order.dto.projection.OfflineProductIdQuantityProjection;
import com.shop.order.repository.OrderProductRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 오프라인 매출 upsert 시 title별 출고 수량 증분을 OfflineProduct에 반영한다.
 * 드리프트/cutover 보정용 전량 절대값 재집계도 제공한다.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OfflineShippingQuantityAdjuster {

    private final OfflineProductRepository offlineProductRepository;
    private final OfflineSalesItemRepository offlineSalesItemRepository;
    private final OrderProductRepository orderProductRepository;
    private final OfflinePackagingConversionService packagingConversionService;
    private final OfflineReceivingOnlineStockAdjuster receivingOnlineStockAdjuster;

    /**
     * title → 수량 델타를 OfflineProduct.shippingQuantity에 가감한다.
     * 매칭 상품이 없으면 skip + warn. 동일 title이 여러 개면 첫 건만 반영한다.
     */
    @Transactional
    public void applyTitleQuantityDeltas(Map<String, Integer> deltas) {
        if (deltas == null || deltas.isEmpty()) {
            return;
        }
        Map<String, Integer> nonZero = new HashMap<>();
        for (Map.Entry<String, Integer> entry : deltas.entrySet()) {
            String title = entry.getKey();
            Integer delta = entry.getValue();
            if (title == null || title.isBlank() || delta == null || delta == 0) {
                continue;
            }
            nonZero.put(title, delta);
        }
        if (nonZero.isEmpty()) {
            return;
        }

        Set<String> titles = nonZero.keySet();
        Map<String, List<OfflineProduct>> byTitle = offlineProductRepository.findAllByTitleIn(titles).stream()
                .collect(Collectors.groupingBy(OfflineProduct::getTitle));

        for (Map.Entry<String, Integer> entry : nonZero.entrySet()) {
            String title = entry.getKey();
            int delta = entry.getValue();
            List<OfflineProduct> products = byTitle.getOrDefault(title, List.of());
            if (products.isEmpty()) {
                log.warn("출고 반영 대상 OfflineProduct 없음: title={}, delta={}", title, delta);
                continue;
            }
            if (products.size() > 1) {
                log.warn("동일 title OfflineProduct 복수 — 첫 건만 출고 반영: title={}, count={}",
                        title, products.size());
            }
            OfflineProduct product = products.get(0);
            int current = product.getShippingQuantity() != null ? product.getShippingQuantity() : 0;
            product.setShippingQuantity(current + delta);
            offlineProductRepository.save(product);
            if (delta > 0) {
                packagingConversionService.ensurePackagingStockAfterShipping(product);
            }
            receivingOnlineStockAdjuster.syncOnlineStockFromOffline(product);
        }
    }

    /**
     * shippingQuantity를 절대값 SET 한다.
     * 목표값 = (title별 COMPLETED 오프라인 매출 합, 동일 title 첫 건만) + (해당 productId에 링크된 비취소 온라인 주문 수량).
     * <p>참고: restoreStock=false 취소는 실시간 경로에서 shipping을 유지하지만,
     * 비취소 주문만 합산하는 reconcile과는 어긋날 수 있다.</p>
     */
    @Transactional
    public OfflineShippingReconcileResultDto reconcileAbsoluteShippingQuantities() {
        Map<String, Integer> salesByTitle = new HashMap<>();
        for (TitleQuantityProjection row : offlineSalesItemRepository.sumCompletedQuantityGroupedByTitle()) {
            if (row == null || row.getTitle() == null || row.getTitle().isBlank()) {
                continue;
            }
            int qty = row.getTotalQuantity() != null ? row.getTotalQuantity().intValue() : 0;
            salesByTitle.put(row.getTitle(), qty);
        }

        Map<String, Integer> onlineByOfflineProductId = new HashMap<>();
        for (OfflineProductIdQuantityProjection row
                : orderProductRepository.sumActiveOnlineQuantityGroupedByOfflineProductId()) {
            if (row == null || !StringUtils.hasText(row.getOfflineProductId())) {
                continue;
            }
            int qty = row.getTotalQuantity() != null ? row.getTotalQuantity().intValue() : 0;
            onlineByOfflineProductId.put(row.getOfflineProductId(), qty);
        }

        List<OfflineProduct> allProducts = offlineProductRepository.findAll();
        Map<String, List<OfflineProduct>> byTitle = allProducts.stream()
                .filter(p -> p.getTitle() != null && !p.getTitle().isBlank())
                .collect(Collectors.groupingBy(OfflineProduct::getTitle));

        Set<String> productTitles = byTitle.keySet();
        int unmatchedSalesTitleCount = 0;
        for (String salesTitle : salesByTitle.keySet()) {
            if (!productTitles.contains(salesTitle)) {
                unmatchedSalesTitleCount++;
                log.warn("출고 재집계: OfflineProduct 미매칭 title={}, qty={}",
                        salesTitle, salesByTitle.get(salesTitle));
            }
        }

        int duplicateTitleGroupCount = 0;
        int updatedProductCount = 0;
        Set<Long> touchedIds = new HashSet<>();

        for (Map.Entry<String, List<OfflineProduct>> entry : byTitle.entrySet()) {
            List<OfflineProduct> products = entry.getValue().stream()
                    .sorted(Comparator.comparing(OfflineProduct::getId))
                    .toList();
            if (products.size() > 1) {
                duplicateTitleGroupCount++;
                log.warn("출고 재집계: 동일 title 복수 — 오프라인 매출은 첫 건만, 온라인은 productId별: title={}, count={}",
                        entry.getKey(), products.size());
            }
            int offlineExpected = salesByTitle.getOrDefault(entry.getKey(), 0);
            for (int i = 0; i < products.size(); i++) {
                OfflineProduct product = products.get(i);
                int offlinePart = (i == 0) ? offlineExpected : 0;
                int onlinePart = onlineQtyFor(product, onlineByOfflineProductId);
                int target = offlinePart + onlinePart;
                if (setShippingIfChanged(product, target)) {
                    updatedProductCount++;
                    touchedIds.add(product.getId());
                }
            }
        }

        for (OfflineProduct product : allProducts) {
            if (product.getTitle() != null && !product.getTitle().isBlank()) {
                continue;
            }
            if (touchedIds.contains(product.getId())) {
                continue;
            }
            int target = onlineQtyFor(product, onlineByOfflineProductId);
            if (setShippingIfChanged(product, target)) {
                updatedProductCount++;
            }
        }

        for (OfflineProduct product : allProducts) {
            packagingConversionService.ensurePackagingStockAfterShipping(product);
        }
        for (OfflineProduct product : allProducts) {
            receivingOnlineStockAdjuster.syncOnlineStockFromOffline(product);
        }

        return OfflineShippingReconcileResultDto.builder()
                .updatedProductCount(updatedProductCount)
                .salesTitleCount(salesByTitle.size())
                .unmatchedSalesTitleCount(unmatchedSalesTitleCount)
                .duplicateTitleGroupCount(duplicateTitleGroupCount)
                .build();
    }

    /**
     * 현재 DB의 입고/출고만 사용한다. 출고 절대값 재집계 없이
     * 묶음 포장 전환 후 링크된 온라인 재고를 맞춘다.
     * (입고−출고 ≤ 0이면 온라인 재고는 변경하지 않음)
     */
    @Transactional
    public OfflineStockSyncResultDto syncStockFromCurrentQuantities() {
        List<OfflineProduct> allProducts = offlineProductRepository.findAll();
        for (OfflineProduct product : allProducts) {
            packagingConversionService.ensurePackagingStockAfterShipping(product);
        }
        for (OfflineProduct product : allProducts) {
            receivingOnlineStockAdjuster.syncOnlineStockFromOffline(product);
        }
        return OfflineStockSyncResultDto.builder()
                .processedProductCount(allProducts.size())
                .build();
    }

    private static int onlineQtyFor(OfflineProduct product, Map<String, Integer> onlineByOfflineProductId) {
        if (product == null || !StringUtils.hasText(product.getProductId())) {
            return 0;
        }
        return onlineByOfflineProductId.getOrDefault(product.getProductId(), 0);
    }

    private static boolean setShippingIfChanged(OfflineProduct product, int target) {
        int current = product.getShippingQuantity() != null ? product.getShippingQuantity() : 0;
        if (product.getShippingQuantity() != null && current == target) {
            return false;
        }
        product.setShippingQuantity(target);
        return true;
    }

    /**
     * before/after title→수량 맵의 차이를 계산한다.
     */
    public static Map<String, Integer> diffContributions(
            Map<String, Integer> before,
            Map<String, Integer> after) {
        Map<String, Integer> beforeSafe = before != null ? before : Map.of();
        Map<String, Integer> afterSafe = after != null ? after : Map.of();
        Map<String, Integer> deltas = new HashMap<>();
        for (String title : beforeSafe.keySet()) {
            int b = beforeSafe.getOrDefault(title, 0);
            int a = afterSafe.getOrDefault(title, 0);
            int delta = a - b;
            if (delta != 0) {
                deltas.put(title, delta);
            }
        }
        for (String title : afterSafe.keySet()) {
            if (beforeSafe.containsKey(title)) {
                continue;
            }
            int a = afterSafe.getOrDefault(title, 0);
            if (a != 0) {
                deltas.put(title, a);
            }
        }
        return deltas;
    }
}
