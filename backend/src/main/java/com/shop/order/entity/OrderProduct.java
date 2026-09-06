package com.shop.order.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import com.shop.search.dto.enums.ProductTableEnum;

@Entity
@Table(name = "order_products", indexes = {
    @Index(name = "idx_order_products_order_info_id", columnList = "order_info_id"),
    @Index(name = "idx_order_products_search_map_id", columnList = "search_map_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderProduct {

    // 주문 상품 엔티티

    // 주문 상품 ID
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 주문 정보 ID
    @Column(nullable = false)
    private Long orderInfoId;

    // 상품 ID
    @Column(nullable = false)
    private Long productId;

    /**
     * {@code productId}가 가리키는 엔티티 종류. null인 행은 레거시로 {@link ProductTableEnum#UNION_PRICE}로 간주.
     */
    @Column(name = "product_table", length = 40)
    @Enumerated(EnumType.STRING)
    private ProductTableEnum productTable;

    // 상품 수량
    @Column(nullable = false)
    private Long quantity;

    // 상품 가격
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal price;

    // 상품 총 가격
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal totalPrice;

    @Column(nullable = true)
    private Long searchMapId;

    @Column(nullable = true, length = 26)
    private String productPublicId;

    @Column(nullable = true, length = 512)
    private String productNameKo;

    @Column(nullable = true, length = 512)
    private String productNameEn;

    @Column(nullable = true, length = 1024)
    private String imageUrl;

    @Column(nullable = true, length = 64)
    private String productType;

    /** 주문 시점 단가 스냅샷 */
    @Column(nullable = true)
    private Long snapshotUnitPrice;

    /** 적립금 (주문 시점 규칙 적용 결과, null = 적립 없음) */
    @Column(nullable = true)
    private Long rewardPoints;
}
