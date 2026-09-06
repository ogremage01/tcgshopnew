package com.shop.product.service.supply;

import java.util.List;
import java.util.stream.Collectors;

import java.io.IOException;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.shop.common.fileUpload.service.FileUploadService;
import com.shop.offline.product.entity.OfflineProduct;
import com.shop.offline.product.repository.OfflineProductRepository;
import com.shop.product.dto.supplies.AddSupplyProductDto;
import com.shop.product.dto.supplies.MakerCreateRequestDto;
import com.shop.product.dto.supplies.MakerDto;
import com.shop.product.dto.supplies.SupplyDto;
import com.shop.product.dto.supplies.SupplyTypeCreateRequestDto;
import com.shop.product.dto.supplies.SupplyTypeDto;
import com.shop.product.dto.supplies.UpdateSupplyProductDto;
import com.shop.product.entity.supplies.Maker;
import com.shop.product.entity.supplies.Supply;
import com.shop.product.entity.supplies.SupplyType;
import com.shop.product.repository.supply.MakerRepository;
import com.shop.product.repository.supply.SupplyRepository;
import com.shop.product.repository.supply.SupplyTypeRepository;

import com.shop.search.service.ProductSearchMapStockSyncPublisher;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminSupplyServiceImpl implements AdminSupplyService {

    private final MakerRepository makerRepository;
    private final SupplyTypeRepository supplyTypeRepository;
    private final SupplyRepository supplyRepository;
    private final FileUploadService fileUploadService;
    private final ProductSearchMapStockSyncPublisher productSearchMapStockSyncPublisher;
    private final OfflineProductRepository offlineProductRepository;

    @Override
    @Transactional
    public void createSupply(AddSupplyProductDto request) throws IOException {
        Maker maker = makerRepository.findById(request.getMakerId())
                .filter(m -> !Boolean.TRUE.equals(m.getIsDeleted()))
                .orElseThrow(() -> new RuntimeException("제조사를 찾을 수 없습니다."));
        SupplyType supplyType = supplyTypeRepository.findById(request.getSupplyTypeId())
                .filter(st -> !Boolean.TRUE.equals(st.getIsDeleted()))
                .orElseThrow(() -> new RuntimeException("서플라이 타입을 찾을 수 없습니다."));

        String imageUrl = null;
        if (request.getImageFile() != null && !request.getImageFile().isEmpty()) {
            imageUrl = fileUploadService.uploadFile(request.getImageFile(), "supplies");
        }

        Supply supply = Supply.builder()
                .nameEn(request.getNameEn())
                .nameKo(request.getNameKo())
                .description(request.getDescription())
                .price(request.getPrice())
                .stock(request.getStock())
                .supplyType(supplyType.getNameKo())
                .maker(maker.getName())
                .imgUrl(imageUrl)
                .isDeleted(false)
                .isVisible(true)
                .offlineProductId(normalizeOfflineProductId(request.getOfflineProductId()))
                .build();

        supplyRepository.save(supply);
        linkOfflineProduct(supply.getOfflineProductId(), "Supply", supply.getId());
        productSearchMapStockSyncPublisher.publishSupplyStockChanged(supply.getId());
    }

    private void linkOfflineProduct(String offlineProductId, String tableName, Long linkId) {
        if (!StringUtils.hasText(offlineProductId)) return;
        offlineProductRepository.findByProductId(offlineProductId).ifPresent(op -> {
            op.setLinkTableName(tableName);
            op.setLinkId(linkId);
            offlineProductRepository.save(op);
        });
    }

    private void unlinkOfflineProduct(String tableName, Long linkId) {
        offlineProductRepository.findByLinkTableNameAndLinkId(tableName, linkId).ifPresent(op -> {
            op.setLinkTableName(null);
            op.setLinkId(null);
            offlineProductRepository.save(op);
        });
    }

    @Override
    public Page<SupplyDto> getSupplyListForAdmin(Pageable pageable, String keyword) {
        Page<Supply> page = (keyword == null || keyword.isBlank())
                ? supplyRepository.findAllForAdmin(pageable)
                : supplyRepository.searchForAdmin(keyword.trim(), pageable);
        return page.map(this::toAdminDto);
    }

    @Override
    public SupplyDto getSupplyByIdForAdmin(Long id) {
        return toAdminDto(findActiveSupply(id));
    }

    @Override
    @Transactional
    public void updateSupply(Long id, UpdateSupplyProductDto request) throws IOException {
        Supply supply = findActiveSupply(id);

        if (request.getNameEn() != null) {
            supply.setNameEn(request.getNameEn());
        }
        if (request.getNameKo() != null) {
            supply.setNameKo(request.getNameKo());
        }
        if (request.getDescription() != null) {
            supply.setDescription(request.getDescription());
        }
        if (request.getPrice() != null) {
            supply.setPrice(request.getPrice());
        }
        if (request.getStock() != null) {
            supply.setStock(request.getStock());
        }
        if (request.getIsVisible() != null) {
            supply.setIsVisible(request.getIsVisible());
        }
        if (request.getMakerId() != null) {
            Maker maker = makerRepository.findById(request.getMakerId())
                    .filter(m -> !Boolean.TRUE.equals(m.getIsDeleted()))
                    .orElseThrow(() -> new RuntimeException("제조사를 찾을 수 없습니다."));
            supply.setMaker(maker.getName());
        }
        if (request.getSupplyTypeId() != null) {
            SupplyType supplyType = supplyTypeRepository.findById(request.getSupplyTypeId())
                    .filter(st -> !Boolean.TRUE.equals(st.getIsDeleted()))
                    .orElseThrow(() -> new RuntimeException("서플라이 타입을 찾을 수 없습니다."));
            supply.setSupplyType(supplyType.getNameKo());
        }
        if (request.getImageFile() != null && !request.getImageFile().isEmpty()) {
            supply.setImgUrl(fileUploadService.uploadFile(request.getImageFile(), "supplies"));
        }
        supply.setOfflineProductId(normalizeOfflineProductId(request.getOfflineProductId()));

        supplyRepository.save(supply);
        productSearchMapStockSyncPublisher.publishSupplyStockChanged(supply.getId());
    }

    @Override
    @Transactional
    public void deleteSupply(Long id) {
        Supply supply = findActiveSupply(id);
        supply.setIsDeleted(true);
        supply.setIsVisible(false);
        supplyRepository.save(supply);
        unlinkOfflineProduct("Supply", supply.getId());
        productSearchMapStockSyncPublisher.publishSupplyStockChanged(supply.getId());
    }

    @Override
    @Transactional
    public Long simpleRegisterFromOffline(OfflineProduct offlineProduct) {
        String title = offlineProduct.getTitle();
        Long price = offlineProduct.getPriceValue() != null
                ? offlineProduct.getPriceValue().longValue()
                : null;

        Supply supply = Supply.builder()
                .nameEn(title)
                .nameKo(title)
                .price(price)
                .stock(0L)
                .isDeleted(false)
                .isVisible(false)
                .offlineProductId(offlineProduct.getProductId())
                .build();

        supplyRepository.save(supply);
        linkOfflineProduct(offlineProduct.getProductId(), "Supply", supply.getId());
        productSearchMapStockSyncPublisher.publishSupplyStockChanged(supply.getId());
        return supply.getId();
    }

    private Supply findActiveSupply(Long id) {
        return supplyRepository.findById(id)
                .filter(s -> !Boolean.TRUE.equals(s.getIsDeleted()))
                .orElseThrow(() -> new RuntimeException("서플라이를 찾을 수 없습니다."));
    }

    private SupplyDto toAdminDto(Supply supply) {
        Long makerId = supply.getMaker() == null ? null
                : makerRepository.findByName(supply.getMaker()).map(Maker::getId).orElse(null);
        Long supplyTypeId = null;
        if (supply.getSupplyType() != null) {
            supplyTypeId = supplyTypeRepository.findByNameKo(supply.getSupplyType())
                    .map(SupplyType::getId)
                    .or(() -> supplyTypeRepository.findByNameEn(supply.getSupplyType()).map(SupplyType::getId))
                    .orElse(null);
        }
        return SupplyDto.builder()
                .id(supply.getId())
                .publicId(supply.getPublicId())
                .nameEn(supply.getNameEn())
                .nameKo(supply.getNameKo())
                .description(supply.getDescription())
                .price(supply.getPrice())
                .stock(supply.getStock())
                .supplyType(supply.getSupplyType())
                .maker(supply.getMaker())
                .makerId(makerId)
                .supplyTypeId(supplyTypeId)
                .imgUrl(supply.getImgUrl())
                .isVisible(supply.getIsVisible())
                .isDeleted(supply.getIsDeleted())
                .offlineProductId(supply.getOfflineProductId())
                .build();
    }

    private String normalizeOfflineProductId(String offlineProductId) {
        return StringUtils.hasText(offlineProductId) ? offlineProductId.trim() : null;
    }

    @Override
    public List<MakerDto> getMakerListForAdmin() {
        return makerRepository.findAllByIsDeleted(false).stream().map(Maker::toDto).collect(Collectors.toList());
    }

    @Override
    public Page<MakerDto> getMakerListByPageForAdmin(Pageable pageable) {
        return makerRepository.findAllByIsDeleted(false, pageable).map(Maker::toDto);
    }

    @Override
    public void createMaker(MakerCreateRequestDto request) {
        Maker maker = Maker.builder()
                .name(request.getName())
                .isDeleted(false)
                .build();
        if (makerRepository.findByName(request.getName()).isPresent()) {
            throw new RuntimeException("제조사 이름이 이미 존재합니다.");
        }
        makerRepository.save(maker);
    }

    @Override
    public void updateMaker(Long id, String name) {
        Maker maker = makerRepository.findById(id).orElseThrow(() -> new RuntimeException("제조사를 찾을 수 없습니다."));
        if (makerRepository.findByName(name).isPresent()) {
            throw new RuntimeException("제조사 이름이 이미 존재합니다.");
        }
        maker.setName(name);
        makerRepository.save(maker);
    }

    @Override
    public void deleteMaker(Long id) {
        Maker maker = makerRepository.findById(id).orElseThrow(() -> new RuntimeException("제조사를 찾을 수 없습니다."));
        maker.setIsDeleted(true);
        makerRepository.save(maker);
    }

    @Override
    public void createSupplyType(SupplyTypeCreateRequestDto request) {
        SupplyType supplyType = SupplyType.builder()
                .nameEn(request.getNameEn())
                .nameKo(request.getNameKo())
                .isDeleted(false)
                .build();
        supplyTypeRepository.save(supplyType);
    }

    @Override
    public void updateSupplyType(Long id, String nameEn, String nameKo) {
        SupplyType supplyType = supplyTypeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("서플라이 타입을 찾을 수 없습니다."));
        supplyType.setNameEn(nameEn != null ? nameEn : supplyType.getNameEn());
        supplyType.setNameKo(nameKo != null ? nameKo : supplyType.getNameKo());
        supplyTypeRepository.save(supplyType);
    }

    @Override
    public void deleteSupplyType(Long id) {
        SupplyType supplyType = supplyTypeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("서플라이 타입을 찾을 수 없습니다."));
        supplyType.setIsDeleted(true);
        supplyTypeRepository.save(supplyType);
    }

    @Override
    public List<SupplyTypeDto> getSupplyTypeListForAdmin() {
        return supplyTypeRepository.findAllByIsDeleted(false).stream().map(SupplyType::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public Page<SupplyTypeDto> getSupplyTypeListByPageForAdmin(Pageable pageable) {
        return supplyTypeRepository.findAllByIsDeleted(false, pageable).map(SupplyType::toDto);
    }

}
