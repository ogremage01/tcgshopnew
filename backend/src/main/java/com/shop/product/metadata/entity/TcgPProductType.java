package com.shop.product.metadata.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import jakarta.persistence.Column;

@Entity
@Table(name = "tcg_p_product_types")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TcgPProductType {

    // TCGPlayer 상품 타입 엔티티(Card/Sealed Product)

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    // 상품 타입 ID
    @Column(unique = true)
    private Long productTypeId;

    // 상품 이름
    private String productName;

    // 게임 라인 ID
    private Long productLineId;
}
