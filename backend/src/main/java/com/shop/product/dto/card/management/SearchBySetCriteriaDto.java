package com.shop.product.dto.card.management;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SearchBySetCriteriaDto {
    private String game;
    private String set;
    /** all | foil | normal */
    private String printTypeFilter;
    private Long storageId;
    private Boolean isVisible;
}
