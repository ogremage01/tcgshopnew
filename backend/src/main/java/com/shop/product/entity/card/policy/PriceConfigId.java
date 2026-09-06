package com.shop.product.entity.card.policy;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class PriceConfigId implements Serializable {

    private static final long serialVersionUID = 1L;

    private String configKey;
    private String configGame;
}
