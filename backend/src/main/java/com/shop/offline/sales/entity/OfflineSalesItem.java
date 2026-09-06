package com.shop.offline.sales.entity;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
@Entity
@Table(name = "offline_sales_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OfflineSalesItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "offline_sales_info_id", nullable = false)
    private OfflineSalesInfo offlineSalesInfo;
    @Column(unique = true, nullable = false)
    private String lineItemId;
    private String title;
    private String category;
    private Integer priceUnit;
    private Integer priceValue;
    private Integer quantity;
    private String memo;
    @OneToMany(mappedBy = "offlineSalesItem", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<OfflineSalesItemDiscount> discounts = new ArrayList<>();
}
