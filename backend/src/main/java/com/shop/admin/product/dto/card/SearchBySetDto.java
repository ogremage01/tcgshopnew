package com.shop.admin.product.dto.card;

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
public class SearchBySetDto {
    private String game;
    private String set;
    /** all | foil | normal */
    private String printTypeFilter;
    private Long storageId;

}
