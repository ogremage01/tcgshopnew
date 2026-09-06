package com.shop.search.event;

import lombok.Getter;
import lombok.AllArgsConstructor;
import lombok.Setter;
import com.shop.product.entity.manualProduct.ManualProduct;

@Getter
@Setter
@AllArgsConstructor
public class ManualProductChangeEvent {

    private ManualProduct manualProduct;

}
