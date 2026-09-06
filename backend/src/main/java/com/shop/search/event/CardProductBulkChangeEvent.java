package com.shop.search.event;

import java.util.List;

import com.shop.product.entity.card.CardProduct;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class CardProductBulkChangeEvent {

    private List<CardProduct> cardProducts;

}
