package com.shop.offline.sales.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.OneToMany;

@Entity
@Table(name = "offline_sales_infos", indexes = {
    @Index(name = "idx_offline_sales_infos_state_created_at", columnList = "order_state,created_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OfflineSalesInfo {

    public static final String ORDER_STATE_COMPLETED = "COMPLETED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(unique = true, nullable = false)
    private String orderId;
    private String orderState;
    private String orderNumber;
    private LocalDateTime createdAt;
    @OneToMany(mappedBy = "offlineSalesInfo", cascade = CascadeType.ALL)
    private List<OfflineSalesItem> lineItems;
    @OneToMany(mappedBy = "offlineSalesInfo", cascade = CascadeType.ALL)
    @Builder.Default
    private List<OfflineSalesPayment> payments = new ArrayList<>();
    private Integer listPrice;
    private Integer discountAmount;
    private Integer taxAmount;
    private Integer supplyAmount;
    private Integer taxExemptAmount;
    private Integer totalAmount;
}
