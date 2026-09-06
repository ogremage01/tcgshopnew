package com.shop.offline.product.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.shop.offline.product.dto.PackagingUnitDto;
import com.shop.offline.product.dto.PackagingUnitRequest;

public interface PackagingUnitService {

    Page<PackagingUnitDto> list(Pageable pageable);

    PackagingUnitDto get(Long id);

    PackagingUnitDto create(PackagingUnitRequest request);

    PackagingUnitDto update(Long id, PackagingUnitRequest request);

    void delete(Long id);
}
