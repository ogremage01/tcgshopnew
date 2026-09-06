package com.shop.search.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import com.shop.product.entity.sealedProduct.SealedProduct;

@Getter
@Setter
@AllArgsConstructor
public class SealedProductChangeEvent {

    private SealedProduct sealedProduct;
}
