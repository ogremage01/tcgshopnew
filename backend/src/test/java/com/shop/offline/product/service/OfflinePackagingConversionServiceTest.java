package com.shop.offline.product.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.shop.offline.product.entity.OfflineProduct;
import com.shop.offline.product.entity.OfflineProductReceivingHistory;
import com.shop.offline.product.entity.PackagingUnit;
import com.shop.offline.product.repository.OfflineProductReceivingHistoryRepository;
import com.shop.offline.product.repository.OfflineProductRepository;
import com.shop.offline.product.repository.PackagingUnitRepository;

@ExtendWith(MockitoExtension.class)
class OfflinePackagingConversionServiceTest {

    @Mock
    private PackagingUnitRepository packagingUnitRepository;
    @Mock
    private OfflineProductRepository offlineProductRepository;
    @Mock
    private OfflineProductReceivingHistoryRepository receivingHistoryRepository;
    @Mock
    private OfflineReceivingOnlineStockAdjuster receivingOnlineStockAdjuster;

    @InjectMocks
    private OfflinePackagingConversionService service;

    @Test
    @DisplayName("묶음 재고 부족 시 낱개→묶음 포장: packaging +1, piece -10, SYSTEM 이력")
    void ensurePackagingStock_packsFromPieces() {
        PackagingUnit unit = PackagingUnit.builder()
                .pieceId(1L).packagingId(2L).unitCount(10L).build();
        when(packagingUnitRepository.findByPackagingId(2L)).thenReturn(Optional.of(unit));

        OfflineProduct packaging = OfflineProduct.builder()
                .id(2L).receivingQuantity(0).shippingQuantity(1).build();
        OfflineProduct piece = OfflineProduct.builder()
                .id(1L).receivingQuantity(25).shippingQuantity(0).build();
        when(offlineProductRepository.findById(1L)).thenReturn(Optional.of(piece));

        service.ensurePackagingStockAfterShipping(packaging);

        assertThat(packaging.getReceivingQuantity()).isEqualTo(1);
        assertThat(piece.getReceivingQuantity()).isEqualTo(15);
        verify(receivingOnlineStockAdjuster).applyReceivingDelta(packaging, 1);
        verify(receivingOnlineStockAdjuster).applyReceivingDelta(piece, -10);

        ArgumentCaptor<OfflineProductReceivingHistory> histCaptor =
                ArgumentCaptor.forClass(OfflineProductReceivingHistory.class);
        verify(receivingHistoryRepository).save(histCaptor.capture());
        OfflineProductReceivingHistory history = histCaptor.getValue();
        assertThat(history.getReceivingManager())
                .isEqualTo(OfflinePackagingConversionService.SYSTEM_RECEIVING_MANAGER);
        assertThat(history.getItems()).hasSize(2);
        assertThat(history.getItems().get(0).getProductId()).isEqualTo(2L);
        assertThat(history.getItems().get(0).getReceivingQuantity()).isEqualTo(1);
        assertThat(history.getItems().get(1).getProductId()).isEqualTo(1L);
        assertThat(history.getItems().get(1).getReceivingQuantity()).isEqualTo(-10);
    }

    @Test
    @DisplayName("낱개 몫이 0이면 포장하지 않는다")
    void ensurePackagingStock_noopWhenPieceInsufficient() {
        PackagingUnit unit = PackagingUnit.builder()
                .pieceId(1L).packagingId(2L).unitCount(10L).build();
        when(packagingUnitRepository.findByPackagingId(2L)).thenReturn(Optional.of(unit));

        OfflineProduct packaging = OfflineProduct.builder()
                .id(2L).receivingQuantity(0).shippingQuantity(1).build();
        OfflineProduct piece = OfflineProduct.builder()
                .id(1L).receivingQuantity(5).shippingQuantity(0).build();
        when(offlineProductRepository.findById(1L)).thenReturn(Optional.of(piece));

        service.ensurePackagingStockAfterShipping(packaging);

        assertThat(packaging.getReceivingQuantity()).isEqualTo(0);
        assertThat(piece.getReceivingQuantity()).isEqualTo(5);
        verify(receivingHistoryRepository, never()).save(any());
        verify(receivingOnlineStockAdjuster, never()).applyReceivingDelta(any(), any(Integer.class));
    }

    @Test
    @DisplayName("묶음 stock >= 0이면 no-op")
    void ensurePackagingStock_noopWhenStockNonNegative() {
        PackagingUnit unit = PackagingUnit.builder()
                .pieceId(1L).packagingId(2L).unitCount(10L).build();
        when(packagingUnitRepository.findByPackagingId(2L)).thenReturn(Optional.of(unit));

        OfflineProduct packaging = OfflineProduct.builder()
                .id(2L).receivingQuantity(2).shippingQuantity(2).build();

        service.ensurePackagingStockAfterShipping(packaging);

        verify(offlineProductRepository, never()).findById(any());
        verify(receivingHistoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("부분 포장: needed 3, availableBoxes 1 → N=1")
    void ensurePackagingStock_partialPack() {
        PackagingUnit unit = PackagingUnit.builder()
                .pieceId(1L).packagingId(2L).unitCount(10L).build();
        when(packagingUnitRepository.findByPackagingId(2L)).thenReturn(Optional.of(unit));

        OfflineProduct packaging = OfflineProduct.builder()
                .id(2L).receivingQuantity(0).shippingQuantity(3).build();
        OfflineProduct piece = OfflineProduct.builder()
                .id(1L).receivingQuantity(10).shippingQuantity(0).build();
        when(offlineProductRepository.findById(1L)).thenReturn(Optional.of(piece));

        service.ensurePackagingStockAfterShipping(packaging);

        assertThat(packaging.getReceivingQuantity()).isEqualTo(1);
        assertThat(piece.getReceivingQuantity()).isEqualTo(0);
        verify(receivingOnlineStockAdjuster).applyReceivingDelta(eq(packaging), eq(1));
        verify(receivingOnlineStockAdjuster).applyReceivingDelta(eq(piece), eq(-10));
        // stock still negative: 1 - 3 = -2
        assertThat(packaging.getReceivingQuantity() - packaging.getShippingQuantity()).isEqualTo(-2);
    }

    @Test
    @DisplayName("packagingId 매핑 없으면 no-op")
    void ensurePackagingStock_noopWithoutMapping() {
        when(packagingUnitRepository.findByPackagingId(2L)).thenReturn(Optional.empty());

        OfflineProduct packaging = OfflineProduct.builder()
                .id(2L).receivingQuantity(0).shippingQuantity(1).build();

        service.ensurePackagingStockAfterShipping(packaging);

        verify(offlineProductRepository, never()).findById(any());
        verify(receivingHistoryRepository, never()).save(any());
    }
}
