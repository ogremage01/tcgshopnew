package com.shop.order.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "order_info_snapshots")
public class OrderInfoSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 원본 OrderInfo ID (참조용, FK 아님) */
    @Column(nullable = false)
    private Long originalOrderInfoId;

    /** 스냅샷 생성 시각 */
    @Column(nullable = false)
    private LocalDateTime snapshotAt;

    /** 수정 주체 (향후 확장용) */
    @Column(nullable = true, length = 128)
    private String modifiedBy;

    @Column(nullable = true, precision = 19, scale = 4)
    private BigDecimal totalProductAmount;

    @Column(nullable = true, precision = 19, scale = 4)
    private BigDecimal deliveryFee;

    @Column(nullable = true, precision = 19, scale = 4)
    private BigDecimal usedPointAmount;

    @Column(nullable = true, precision = 19, scale = 4)
    private BigDecimal actualPaymentAmount;

    @Column(nullable = true)
    private Long totalQuantity;

    @Column(nullable = true)
    private Long orderLineCount;

    @Column(nullable = true, length = 64)
    private String orderStatus;
}
