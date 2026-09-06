package com.shop.product.service.supply;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.shop.offline.product.entity.OfflineProduct;
import com.shop.product.dto.supplies.AddSupplyProductDto;
import com.shop.product.dto.supplies.MakerCreateRequestDto;
import com.shop.product.dto.supplies.MakerDto;
import com.shop.product.dto.supplies.SupplyDto;
import com.shop.product.dto.supplies.SupplyTypeCreateRequestDto;
import com.shop.product.dto.supplies.SupplyTypeDto;
import com.shop.product.dto.supplies.UpdateSupplyProductDto;

import java.io.IOException;

public interface AdminSupplyService {

    // ------------------------------------------------------
    // 서플라이 상품 관리
    // ------------------------------------------------------
    void createSupply(AddSupplyProductDto request) throws IOException;

    Page<SupplyDto> getSupplyListForAdmin(Pageable pageable, String keyword);

    SupplyDto getSupplyByIdForAdmin(Long id);

    void updateSupply(Long id, UpdateSupplyProductDto request) throws IOException;

    void deleteSupply(Long id);

    Long simpleRegisterFromOffline(OfflineProduct offlineProduct);

    // ------------------------------------------------------
    // 서플라이 제조사 관리
    // ------------------------------------------------------
    void createMaker(MakerCreateRequestDto request);

    void updateMaker(Long id, String name);

    void deleteMaker(Long id);

    List<MakerDto> getMakerListForAdmin();

    Page<MakerDto> getMakerListByPageForAdmin(Pageable pageable);

    // ------------------------------------------------------
    // 서플라이 타입 관리
    // ------------------------------------------------------

    void createSupplyType(SupplyTypeCreateRequestDto request);

    void updateSupplyType(Long id, String nameEn, String nameKo);

    void deleteSupplyType(Long id);

    List<SupplyTypeDto> getSupplyTypeListForAdmin();

    Page<SupplyTypeDto> getSupplyTypeListByPageForAdmin(Pageable pageable);
}
