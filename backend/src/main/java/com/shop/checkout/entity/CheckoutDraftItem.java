package com.shop.checkout.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "checkout_draft_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CheckoutDraftItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "draft_id", nullable = false)
    private CheckoutDraft draft;

    @Column(nullable = false)
    private Long cartItemId;

    @Column(nullable = false)
    private Long cartItemQuantity;

    @Column(nullable = true)
    private LocalDateTime cartItemUpdatedAt;

    @Column(nullable = false)
    private Long searchMapId;

    @Column(nullable = false, length = 50)
    private String tableName;

    @Column(nullable = false)
    private Long sourceId;

    @Column(nullable = false, length = 26)
    private String productPublicId;

    @Column(nullable = false, length = 64)
    private String productType;

    @Column(length = 512)
    private String productNameEn;

    @Column(length = 512)
    private String productNameKo;

    @Column(length = 1024)
    private String imageUrl;

    @Column(length = 1024)
    private String imageUrlEn;

    @Column(length = 1024)
    private String imageUrlKo;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal snapshotUnitPrice;

    @Column(nullable = false)
    private Long quantity;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal snapshotTotalPrice;

    @Column(nullable = true)
    private Long snapshotPointAmount;
}
