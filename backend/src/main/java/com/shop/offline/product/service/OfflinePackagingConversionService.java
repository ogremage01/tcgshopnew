package com.shop.offline.product.service;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shop.offline.product.entity.OfflineProduct;
import com.shop.offline.product.entity.OfflineProductReceivingHistory;
import com.shop.offline.product.entity.OfflineProductReceivingItem;
import com.shop.offline.product.entity.PackagingUnit;
import com.shop.offline.product.repository.OfflineProductReceivingHistoryRepository;
import com.shop.offline.product.repository.OfflineProductRepository;
import com.shop.offline.product.repository.PackagingUnitRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 묶음(packaging) 출고 후 재고 부족 시 낱개를 묶음으로 SYSTEM 포장 전환한다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OfflinePackagingConversionService {

    public static final String SYSTEM_RECEIVING_MANAGER = "SYSTEM";

    private final PackagingUnitRepository packagingUnitRepository;
    private final OfflineProductRepository offlineProductRepository;
    private final OfflineProductReceivingHistoryRepository receivingHistoryRepository;
    private final OfflineReceivingOnlineStockAdjuster receivingOnlineStockAdjuster;

    /**
     * 출고 반영 후 packaging 재고(receiving - shipping)가 음수이면,
     * 연결된 낱개 재고로 구성 가능한 만큼 박스를 SYSTEM 입고한다 (묶음 +, 낱개 −).
     * packagingId 매핑이 없으면 no-op.
     */
    @Transactional
    public void ensurePackagingStockAfterShipping(OfflineProduct packagingProduct) {
        if (packagingProduct == null || packagingProduct.getId() == null) {
            return;
        }
        Optional<PackagingUnit> unitOpt = packagingUnitRepository.findByPackagingId(packagingProduct.getId());
        if (unitOpt.isEmpty()) {
            return;
        }
        PackagingUnit unit = unitOpt.get();
        long unitCountLong = unit.getUnitCount() != null ? unit.getUnitCount() : 0L;
        if (unitCountLong < 1) {
            log.warn("포장 전환 skip — unitCount 무효: packagingId={}, unitCount={}",
                    packagingProduct.getId(), unitCountLong);
            return;
        }
        int unitCount = (int) unitCountLong;

        int stock = stockOf(packagingProduct);
        if (stock >= 0) {
            return;
        }

        OfflineProduct piece = offlineProductRepository.findById(unit.getPieceId()).orElse(null);
        if (piece == null) {
            log.warn("포장 전환 skip — piece OfflineProduct 없음: pieceId={}", unit.getPieceId());
            return;
        }

        int needed = -stock;
        int pieceStock = stockOf(piece);
        int availableBoxes = Math.max(pieceStock, 0) / unitCount;
        int boxesToPack = Math.min(needed, availableBoxes);
        if (boxesToPack < 1) {
            log.warn(
                    "포장 전환 불가 — 낱개 재고 부족: packagingId={}, pieceId={}, needed={}, pieceStock={}, unitCount={}",
                    packagingProduct.getId(), piece.getId(), needed, pieceStock, unitCount);
            return;
        }

        int pieceDeduct = boxesToPack * unitCount;
        applyReceivingDelta(packagingProduct, boxesToPack);
        applyReceivingDelta(piece, -pieceDeduct);

        OfflineProductReceivingHistory history = OfflineProductReceivingHistory.builder()
                .receivingManager(SYSTEM_RECEIVING_MANAGER)
                .build();
        history.addItem(OfflineProductReceivingItem.builder()
                .productId(packagingProduct.getId())
                .receivingQuantity(boxesToPack)
                .build());
        history.addItem(OfflineProductReceivingItem.builder()
                .productId(piece.getId())
                .receivingQuantity(-pieceDeduct)
                .build());
        receivingHistoryRepository.save(history);

        int afterStock = stockOf(packagingProduct);
        if (afterStock < 0) {
            log.warn(
                    "포장 전환 후에도 묶음 재고 부족: packagingId={}, pieceId={}, packed={}, stock={}",
                    packagingProduct.getId(), piece.getId(), boxesToPack, afterStock);
        }
    }

    private void applyReceivingDelta(OfflineProduct product, int qty) {
        int current = product.getReceivingQuantity() != null ? product.getReceivingQuantity() : 0;
        product.setReceivingQuantity(current + qty);
        offlineProductRepository.save(product);
        receivingOnlineStockAdjuster.applyReceivingDelta(product, qty);
    }

    private static int stockOf(OfflineProduct product) {
        int receiving = product.getReceivingQuantity() != null ? product.getReceivingQuantity() : 0;
        int shipping = product.getShippingQuantity() != null ? product.getShippingQuantity() : 0;
        return receiving - shipping;
    }
}
