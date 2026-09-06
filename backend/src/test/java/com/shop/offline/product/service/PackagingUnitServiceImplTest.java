package com.shop.offline.product.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.shop.offline.product.dto.PackagingUnitDto;
import com.shop.offline.product.dto.PackagingUnitRequest;
import com.shop.offline.product.entity.OfflineProduct;
import com.shop.offline.product.entity.PackagingUnit;
import com.shop.offline.product.repository.OfflineProductRepository;
import com.shop.offline.product.repository.PackagingUnitRepository;

@ExtendWith(MockitoExtension.class)
class PackagingUnitServiceImplTest {

    @Mock
    private PackagingUnitRepository packagingUnitRepository;
    @Mock
    private OfflineProductRepository offlineProductRepository;

    @InjectMocks
    private PackagingUnitServiceImpl service;

    @Test
    @DisplayName("생성: 정상 등록")
    void create_success() {
        PackagingUnitRequest request = PackagingUnitRequest.builder()
                .pieceId(1L).packagingId(2L).unitCount(10L).build();
        when(offlineProductRepository.existsById(1L)).thenReturn(true);
        when(offlineProductRepository.existsById(2L)).thenReturn(true);
        when(packagingUnitRepository.existsByPieceId(1L)).thenReturn(false);
        when(packagingUnitRepository.existsByPackagingId(2L)).thenReturn(false);
        when(packagingUnitRepository.save(any())).thenAnswer(inv -> {
            PackagingUnit u = inv.getArgument(0);
            u.setId(99L);
            return u;
        });
        when(offlineProductRepository.findAllById(any())).thenReturn(List.of(
                OfflineProduct.builder().id(1L).title("낱개").productId("P1").build(),
                OfflineProduct.builder().id(2L).title("박스").productId("P2").build()));

        PackagingUnitDto dto = service.create(request);

        assertThat(dto.getId()).isEqualTo(99L);
        assertThat(dto.getPieceTitle()).isEqualTo("낱개");
        assertThat(dto.getPackagingTitle()).isEqualTo("박스");
        assertThat(dto.getUnitCount()).isEqualTo(10L);
    }

    @Test
    @DisplayName("생성: piece/packaging 동일하면 거부")
    void create_rejectsSameIds() {
        PackagingUnitRequest request = PackagingUnitRequest.builder()
                .pieceId(1L).packagingId(1L).unitCount(10L).build();

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("달라야");
        verify(packagingUnitRepository, never()).save(any());
    }

    @Test
    @DisplayName("생성: 중복 pieceId 거부")
    void create_rejectsDuplicatePiece() {
        PackagingUnitRequest request = PackagingUnitRequest.builder()
                .pieceId(1L).packagingId(2L).unitCount(10L).build();
        when(offlineProductRepository.existsById(1L)).thenReturn(true);
        when(offlineProductRepository.existsById(2L)).thenReturn(true);
        when(packagingUnitRepository.existsByPieceId(1L)).thenReturn(true);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("이미 등록된 낱개");
    }

    @Test
    @DisplayName("수정: 자기 자신 제외하고 중복 검사")
    void update_allowsSamePieceOnSelf() {
        PackagingUnit existing = PackagingUnit.builder()
                .id(5L).pieceId(1L).packagingId(2L).unitCount(10L).build();
        when(packagingUnitRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(offlineProductRepository.existsById(1L)).thenReturn(true);
        when(offlineProductRepository.existsById(3L)).thenReturn(true);
        when(packagingUnitRepository.existsByPieceIdAndIdNot(1L, 5L)).thenReturn(false);
        when(packagingUnitRepository.existsByPackagingIdAndIdNot(3L, 5L)).thenReturn(false);
        when(offlineProductRepository.findAllById(any())).thenReturn(List.of(
                OfflineProduct.builder().id(1L).title("낱개").productId("P1").build(),
                OfflineProduct.builder().id(3L).title("새박스").productId("P3").build()));

        PackagingUnitDto dto = service.update(5L, PackagingUnitRequest.builder()
                .pieceId(1L).packagingId(3L).unitCount(12L).build());

        assertThat(dto.getPackagingId()).isEqualTo(3L);
        assertThat(dto.getUnitCount()).isEqualTo(12L);
        assertThat(existing.getPackagingId()).isEqualTo(3L);
    }
}
