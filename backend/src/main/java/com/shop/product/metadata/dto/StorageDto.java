package com.shop.product.metadata.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StorageDto {

    // 저장소 정보 DtoS

    private Long id;
    private String storageName;
    private String description;
    private Boolean isDefault;
}
