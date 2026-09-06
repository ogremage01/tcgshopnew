package com.shop.offline.sales.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "offline_sales_payments")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OfflineSalesPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "offline_sales_info_id", nullable = false)
    private OfflineSalesInfo offlineSalesInfo;

    @Column(unique = true, nullable = false)
    private String paymentKey;

    private String sourceType;
    private Integer amount;
    private Integer taxAmount;
    private Integer supplyAmount;
    private Integer taxExemptAmount;
}
