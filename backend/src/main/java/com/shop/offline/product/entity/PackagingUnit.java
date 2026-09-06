package com.shop.offline.product.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "packaging_units",
        uniqueConstraints = {
            @UniqueConstraint(name = "uk_packaging_units_piece", columnNames = "piece_id"),
            @UniqueConstraint(name = "uk_packaging_units_packaging", columnNames = "packaging_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PackagingUnit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 낱개 단위 OfflineProduct.id */
    @Column(name = "piece_id", nullable = false)
    private Long pieceId;

    /** 포장 단위 OfflineProduct.id */
    @Column(name = "packaging_id", nullable = false)
    private Long packagingId;

    /** 포장 1개당 낱개 수 */
    @Column(name = "unit_count", nullable = false)
    private Long unitCount;
}
