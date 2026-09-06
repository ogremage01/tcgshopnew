package com.shop.offline.product.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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
import com.shop.product.entity.manualProduct.ManualProduct;
import com.shop.product.entity.sealedProduct.SealedProduct;
import com.shop.product.entity.supplies.Supply;
import com.shop.product.repository.manualProduct.ManualProductRepository;
import com.shop.product.repository.sealedProduct.SealedProductRepository;
import com.shop.product.repository.supply.SupplyRepository;
import com.shop.search.service.ProductSearchMapStockSyncPublisher;

@ExtendWith(MockitoExtension.class)
class OfflineReceivingOnlineStockAdjusterTest {

    @Mock
    private ManualProductRepository manualProductRepository;
    @Mock
    private SupplyRepository supplyRepository;
    @Mock
    private SealedProductRepository sealedProductRepository;
    @Mock
    private ProductSearchMapStockSyncPublisher productSearchMapStockSyncPublisher;

    @InjectMocks
    private OfflineReceivingOnlineStockAdjuster adjuster;

    @Test
    @DisplayName("링크 없으면 온라인 재고를 건드리지 않는다")
    void skipWhenNoLink() {
        OfflineProduct offline = OfflineProduct.builder()
                .id(1L)
                .productId("P-1")
                .receivingQuantity(10)
                .shippingQuantity(2)
                .build();

        adjuster.syncOnlineStockFromOffline(offline);

        verifyNoInteractions(manualProductRepository, supplyRepository, sealedProductRepository,
                productSearchMapStockSyncPublisher);
    }

    @Test
    @DisplayName("Manual: stock = 입고-출고")
    void syncManualStock() {
        OfflineProduct offline = OfflineProduct.builder()
                .id(1L)
                .linkTableName("Manual")
                .linkId(10L)
                .receivingQuantity(10)
                .shippingQuantity(3)
                .build();
        ManualProduct manual = ManualProduct.builder().id(10L).stock(99L).build();
        when(manualProductRepository.findById(10L)).thenReturn(Optional.of(manual));

        adjuster.syncOnlineStockFromOffline(offline);

        ArgumentCaptor<ManualProduct> captor = ArgumentCaptor.forClass(ManualProduct.class);
        verify(manualProductRepository).save(captor.capture());
        assertThat(captor.getValue().getStock()).isEqualTo(7L);
        verify(productSearchMapStockSyncPublisher).publishManualProductStockChanged(10L);
    }

    @Test
    @DisplayName("입고-출고가 음수면 온라인 재고를 건드리지 않는다")
    void skipWhenNegativeStock() {
        OfflineProduct offline = OfflineProduct.builder()
                .id(1L)
                .linkTableName("Manual")
                .linkId(10L)
                .receivingQuantity(2)
                .shippingQuantity(5)
                .build();

        adjuster.syncOnlineStockFromOffline(offline);

        verifyNoInteractions(manualProductRepository, supplyRepository, sealedProductRepository,
                productSearchMapStockSyncPublisher);
    }

    @Test
    @DisplayName("입고-출고가 0이면 온라인 재고를 건드리지 않는다")
    void skipWhenZeroStock() {
        OfflineProduct offline = OfflineProduct.builder()
                .id(1L)
                .linkTableName("Manual")
                .linkId(10L)
                .receivingQuantity(5)
                .shippingQuantity(5)
                .build();

        adjuster.syncOnlineStockFromOffline(offline);

        verifyNoInteractions(manualProductRepository, supplyRepository, sealedProductRepository,
                productSearchMapStockSyncPublisher);
    }

    @Test
    @DisplayName("Supply: stock = 입고-출고")
    void syncSupplyStock() {
        OfflineProduct offline = OfflineProduct.builder()
                .id(1L)
                .linkTableName("Supply")
                .linkId(20L)
                .receivingQuantity(8)
                .shippingQuantity(1)
                .build();
        Supply supply = Supply.builder().id(20L).stock(1L).build();
        when(supplyRepository.findById(20L)).thenReturn(Optional.of(supply));

        adjuster.applyReceivingIncrease(offline, 4);

        ArgumentCaptor<Supply> captor = ArgumentCaptor.forClass(Supply.class);
        verify(supplyRepository).save(captor.capture());
        assertThat(captor.getValue().getStock()).isEqualTo(7L);
        verify(productSearchMapStockSyncPublisher).publishSupplyStockChanged(20L);
    }

    @Test
    @DisplayName("Sealed: totalStock=입고-출고, visible은 maxVisible 이하로 clamp")
    void syncSealedStock() {
        OfflineProduct offline = OfflineProduct.builder()
                .id(1L)
                .linkTableName("Sealed")
                .linkId(30L)
                .receivingQuantity(13)
                .shippingQuantity(0)
                .build();
        SealedProduct sealed = new SealedProduct();
        sealed.setId(30L);
        sealed.setTotalStock(10);
        sealed.setCurrentVisibleStock(5);
        sealed.setMaxVisibleStock(8);
        when(sealedProductRepository.findById(30L)).thenReturn(Optional.of(sealed));

        adjuster.syncOnlineStockFromOffline(offline);

        ArgumentCaptor<SealedProduct> captor = ArgumentCaptor.forClass(SealedProduct.class);
        verify(sealedProductRepository).save(captor.capture());
        SealedProduct saved = captor.getValue();
        assertThat(saved.getTotalStock()).isEqualTo(13);
        assertThat(saved.getCurrentVisibleStock()).isEqualTo(8);
        assertThat(saved.getMaxVisibleStock()).isEqualTo(8);
        verify(productSearchMapStockSyncPublisher).publishSealedProductStockChanged(30L);
    }

    @Test
    @DisplayName("Sealed: maxVisible 없으면 visible=total")
    void sealedWithoutMaxVisible() {
        OfflineProduct offline = OfflineProduct.builder()
                .id(1L)
                .linkTableName("Sealed")
                .linkId(30L)
                .receivingQuantity(3)
                .shippingQuantity(0)
                .build();
        SealedProduct sealed = new SealedProduct();
        sealed.setId(30L);
        sealed.setTotalStock(2);
        sealed.setCurrentVisibleStock(10);
        sealed.setMaxVisibleStock(null);
        when(sealedProductRepository.findById(30L)).thenReturn(Optional.of(sealed));

        adjuster.syncOnlineStockFromOffline(offline);

        ArgumentCaptor<SealedProduct> captor = ArgumentCaptor.forClass(SealedProduct.class);
        verify(sealedProductRepository).save(captor.capture());
        assertThat(captor.getValue().getTotalStock()).isEqualTo(3);
        assertThat(captor.getValue().getCurrentVisibleStock()).isEqualTo(3);
    }

    @Test
    @DisplayName("온라인 상품이 없으면 save하지 않는다")
    void skipWhenLinkedProductMissing() {
        OfflineProduct offline = OfflineProduct.builder()
                .id(1L)
                .linkTableName("Manual")
                .linkId(99L)
                .receivingQuantity(1)
                .shippingQuantity(0)
                .build();
        when(manualProductRepository.findById(99L)).thenReturn(Optional.empty());

        adjuster.syncOnlineStockFromOffline(offline);

        verify(manualProductRepository, never()).save(any());
        verifyNoInteractions(productSearchMapStockSyncPublisher);
    }

    @Test
    @DisplayName("이미 동일 재고면 save하지 않는다")
    void skipWhenUnchanged() {
        OfflineProduct offline = OfflineProduct.builder()
                .id(1L)
                .linkTableName("Manual")
                .linkId(10L)
                .receivingQuantity(5)
                .shippingQuantity(2)
                .build();
        ManualProduct manual = ManualProduct.builder().id(10L).stock(3L).build();
        when(manualProductRepository.findById(10L)).thenReturn(Optional.of(manual));

        adjuster.syncOnlineStockFromOffline(offline);

        verify(manualProductRepository, never()).save(any());
        verifyNoInteractions(productSearchMapStockSyncPublisher);
    }

    @Test
    @DisplayName("qty=0이면 온라인 재고를 건드리지 않는다")
    void skipWhenZeroDelta() {
        OfflineProduct offline = OfflineProduct.builder()
                .id(1L)
                .linkTableName("Manual")
                .linkId(10L)
                .receivingQuantity(5)
                .shippingQuantity(0)
                .build();

        adjuster.applyReceivingDelta(offline, 0);

        verifyNoInteractions(manualProductRepository, supplyRepository, sealedProductRepository,
                productSearchMapStockSyncPublisher);
    }

    @Test
    @DisplayName("availableStock: 음수도 그대로 반환")
    void availableStockAllowsNegative() {
        OfflineProduct offline = OfflineProduct.builder()
                .receivingQuantity(1)
                .shippingQuantity(4)
                .build();
        assertThat(OfflineReceivingOnlineStockAdjuster.availableStock(offline)).isEqualTo(-3);
    }
}
