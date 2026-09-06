package com.shop.offline.product.service;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shop.offline.product.dto.PackagingUnitDto;
import com.shop.offline.product.dto.PackagingUnitRequest;
import com.shop.offline.product.entity.OfflineProduct;
import com.shop.offline.product.entity.PackagingUnit;
import com.shop.offline.product.repository.OfflineProductRepository;
import com.shop.offline.product.repository.PackagingUnitRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PackagingUnitServiceImpl implements PackagingUnitService {

    private final PackagingUnitRepository packagingUnitRepository;
    private final OfflineProductRepository offlineProductRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<PackagingUnitDto> list(Pageable pageable) {
        Page<PackagingUnit> page = packagingUnitRepository.findAll(pageable);
        Map<Long, OfflineProduct> productMap = loadProducts(page.getContent());
        return page.map(unit -> toDto(unit, productMap));
    }

    @Override
    @Transactional(readOnly = true)
    public PackagingUnitDto get(Long id) {
        PackagingUnit unit = packagingUnitRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("포장 단위를 찾을 수 없습니다: " + id));
        return toDto(unit, loadProducts(java.util.List.of(unit)));
    }

    @Override
    @Transactional
    public PackagingUnitDto create(PackagingUnitRequest request) {
        validateRequest(request, null);
        PackagingUnit saved = packagingUnitRepository.save(PackagingUnit.builder()
                .pieceId(request.getPieceId())
                .packagingId(request.getPackagingId())
                .unitCount(request.getUnitCount())
                .build());
        return toDto(saved, loadProducts(java.util.List.of(saved)));
    }

    @Override
    @Transactional
    public PackagingUnitDto update(Long id, PackagingUnitRequest request) {
        PackagingUnit unit = packagingUnitRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("포장 단위를 찾을 수 없습니다: " + id));
        validateRequest(request, id);
        unit.setPieceId(request.getPieceId());
        unit.setPackagingId(request.getPackagingId());
        unit.setUnitCount(request.getUnitCount());
        return toDto(unit, loadProducts(java.util.List.of(unit)));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (!packagingUnitRepository.existsById(id)) {
            throw new RuntimeException("포장 단위를 찾을 수 없습니다: " + id);
        }
        packagingUnitRepository.deleteById(id);
    }

    private void validateRequest(PackagingUnitRequest request, Long excludeId) {
        if (request == null) {
            throw new RuntimeException("요청이 필요합니다.");
        }
        if (request.getPieceId() == null || request.getPackagingId() == null) {
            throw new RuntimeException("낱개/포장 상품 ID가 필요합니다.");
        }
        if (request.getPieceId().equals(request.getPackagingId())) {
            throw new RuntimeException("낱개 상품과 포장 상품은 달라야 합니다.");
        }
        if (request.getUnitCount() == null || request.getUnitCount() < 1) {
            throw new RuntimeException("unitCount는 1 이상이어야 합니다.");
        }
        if (!offlineProductRepository.existsById(request.getPieceId())) {
            throw new RuntimeException("낱개 OfflineProduct 없음: " + request.getPieceId());
        }
        if (!offlineProductRepository.existsById(request.getPackagingId())) {
            throw new RuntimeException("포장 OfflineProduct 없음: " + request.getPackagingId());
        }
        boolean pieceTaken = excludeId == null
                ? packagingUnitRepository.existsByPieceId(request.getPieceId())
                : packagingUnitRepository.existsByPieceIdAndIdNot(request.getPieceId(), excludeId);
        if (pieceTaken) {
            throw new RuntimeException("이미 등록된 낱개 상품입니다: " + request.getPieceId());
        }
        boolean packagingTaken = excludeId == null
                ? packagingUnitRepository.existsByPackagingId(request.getPackagingId())
                : packagingUnitRepository.existsByPackagingIdAndIdNot(request.getPackagingId(), excludeId);
        if (packagingTaken) {
            throw new RuntimeException("이미 등록된 포장 상품입니다: " + request.getPackagingId());
        }
    }

    private Map<Long, OfflineProduct> loadProducts(Iterable<PackagingUnit> units) {
        Set<Long> ids = new HashSet<>();
        for (PackagingUnit unit : units) {
            if (unit.getPieceId() != null) {
                ids.add(unit.getPieceId());
            }
            if (unit.getPackagingId() != null) {
                ids.add(unit.getPackagingId());
            }
        }
        if (ids.isEmpty()) {
            return Map.of();
        }
        return offlineProductRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(OfflineProduct::getId, Function.identity()));
    }

    private PackagingUnitDto toDto(PackagingUnit unit, Map<Long, OfflineProduct> productMap) {
        OfflineProduct piece = productMap.get(unit.getPieceId());
        OfflineProduct packaging = productMap.get(unit.getPackagingId());
        return PackagingUnitDto.builder()
                .id(unit.getId())
                .pieceId(unit.getPieceId())
                .pieceTitle(piece != null ? piece.getTitle() : null)
                .pieceProductId(piece != null ? piece.getProductId() : null)
                .packagingId(unit.getPackagingId())
                .packagingTitle(packaging != null ? packaging.getTitle() : null)
                .packagingProductId(packaging != null ? packaging.getProductId() : null)
                .unitCount(unit.getUnitCount())
                .build();
    }
}
