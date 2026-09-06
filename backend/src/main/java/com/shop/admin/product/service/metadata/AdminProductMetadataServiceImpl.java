package com.shop.admin.product.service.metadata;

import java.util.List;

import com.shop.product.metadata.entity.TcgPSetName;
import com.shop.product.metadata.entity.TcgPProductLine;
import com.shop.product.metadata.repository.TcgPSetNameRepository;
import com.shop.product.metadata.repository.TcgPProductLineRepository;
import com.shop.product.metadata.repository.TcgPSyncGameRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.util.stream.Collectors;

import com.shop.product.metadata.entity.TcgPSyncGame;
import com.shop.product.metadata.repository.StorageRepository;
import com.shop.product.dto.card.management.TcgPSyncGameDto;
import com.shop.product.metadata.dto.StorageDto;
import com.shop.product.metadata.dto.TcgPProductLineDto;
import com.shop.product.metadata.entity.Storage;
import org.springframework.transaction.annotation.Transactional;
import com.shop.product.service.CardProductService;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminProductMetadataServiceImpl implements AdminProductMetadataService {
    private final TcgPProductLineRepository productLineRepository;
    private final TcgPSetNameRepository setNameRepository;
    private final TcgPSyncGameRepository syncGameRepository;
    private final StorageRepository storageRepository;
    private final CardProductService cardProductService;

    @Override
    public List<TcgPProductLineDto> getProductLineList() {
        return productLineRepository.findAll(Sort.by(Sort.Direction.ASC, "productLineId")).stream()
                .map(TcgPProductLine::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<TcgPSetName> getSetNameList(Long productLineId) {
        // Derived query: set_names.category_id = productLineId
        return setNameRepository.findAllByCategoryId(productLineId, Sort.by("setNameId").ascending());
    }

    @Override
    public List<TcgPSyncGameDto> getSyncGameList() {
        return syncGameRepository.findAll().stream()
                .map(TcgPSyncGame::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<StorageDto> getStorageList() {
        return storageRepository.findAllByOrderByIsDefaultDescIdAsc();
    }

    @Override
    public void setStorage(StorageDto storageDto) {
        // 기본 보관소 여부 체크
        if (storageDto.getIsDefault()) {
            storageRepository.clearIsDefault();
        }
        Storage storage = Storage.builder()
                .storageName(storageDto.getStorageName())
                .description(storageDto.getDescription())
                .isDefault(storageDto.getIsDefault())
                .build();
        storageRepository.save(storage);
    }

    @Override
    public void updateStorage(StorageDto storageDto) {
        if (storageDto.getIsDefault()) {
            storageRepository.clearIsDefault();
        }
        storageRepository.save(Storage.builder()
                .id(storageDto.getId())
                .storageName(storageDto.getStorageName())
                .description(storageDto.getDescription())
                .isDefault(storageDto.getIsDefault())
                .build());
    }

    @Override
    @Transactional
    public void deleteStorage(StorageDto storageDto) {
        cardProductService.deleteCardProductsByStorageId(storageDto.getId());
        Storage storage = storageRepository.findById(storageDto.getId())
                .orElseThrow(() -> new RuntimeException("Storage not found"));
        storage.setIsDeleted(true);
        storage.setIsDefault(false);
        storageRepository.save(storage);
    }
}
