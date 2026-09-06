package com.shop.product.service.manualProduct;

import com.shop.offline.product.entity.OfflineProduct;
import com.shop.product.dto.manual.AddManualProductDto;
import com.shop.product.dto.manual.ManualProductDto;
import com.shop.product.dto.manual.UpdateManualProductDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.io.IOException;
public interface ManualProductService {

    void createManualProduct(AddManualProductDto addManualProductDto) throws IOException;
    void updateManualProduct(Long id, UpdateManualProductDto request) throws IOException;
    void deleteManualProduct(Long id);
    Page<ManualProductDto> getManualProductList(Pageable pageable);
    Page<ManualProductDto> getManualProductList(String keyword, Pageable pageable);
    ManualProductDto getManualProductById(Long id);

    Long simpleRegisterFromOffline(OfflineProduct offlineProduct);

}
