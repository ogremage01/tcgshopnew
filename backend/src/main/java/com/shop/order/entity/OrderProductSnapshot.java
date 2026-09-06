package com.shop.order.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.shop.search.dto.enums.ProductTableEnum;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "order_product_snapshots")
public class OrderProductSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 상위 OrderInfoSnapshot ID (스냅샷 묶음 키) */
    @Column(nullable = false)
    private Long orderInfoSnapshotId;

    /** 원본 OrderProduct ID (참조용, FK 아님) */
    @Column(nullable = false)
    private Long originalOrderProductId;

    @Column(nullable = true)
    private Long productId;

    @Column(name = "product_table", length = 40)
    @Enumerated(EnumType.STRING)
    private ProductTableEnum productTable;

    @Column(nullable = true)
    private Long quantity;

    @Column(nullable = true, precision = 19, scale = 4)
    private BigDecimal price;

    @Column(nullable = true, precision = 19, scale = 4)
    private BigDecimal totalPrice;

    @Column(nullable = true)
    private Long snapshotUnitPrice;

    @Column(nullable = true, length = 512)
    private String productNameKo;

    @Column(nullable = true, length = 512)
    private String productNameEn;
}
