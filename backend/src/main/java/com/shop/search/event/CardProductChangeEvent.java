package com.shop.search.event;


import lombok.Getter;
import lombok.AllArgsConstructor;
import lombok.Setter;
import com.shop.product.entity.card.CardProduct;

@Getter
@Setter
@AllArgsConstructor

public class CardProductChangeEvent {

    private CardProduct cardProduct;

}
