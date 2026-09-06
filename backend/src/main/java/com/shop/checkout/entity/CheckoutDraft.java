package com.shop.checkout.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.shop.checkout.domain.DeliveryMethod;
import com.shop.checkout.domain.DraftStatus;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "checkout_drafts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class CheckoutDraft {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 26)
    private String publicId;

    @Column(nullable = true)
    private Long userId;

    @Column(nullable = true, length = 512)
    private String guestId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private DraftStatus status;

    @Column(nullable = true)
    private Long confirmedOrderId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = true, length = 32)
    private DeliveryMethod deliveryMethod;

    @Column(length = 255)
    private String recipientName;

    @Column(length = 500)
    private String recipientAddress;

    @Column(length = 500)
    private String recipientAddressDetail;

    @Column(length = 10)
    private String recipientPostalCode;

    @Column(length = 50)
    private String recipientPhone;

    @Column(length = 255)
    private String recipientEmail;

    @Column(length = 500)
    private String orderRequest;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal subtotalAmount;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal deliveryFee;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal usedPointAmount;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal totalAmount;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false, length = 32)
    private String paymentCurrency;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal paymentCurrencyRate;

    @Column(nullable = false, precision = 19, scale = 4)
    private Long settleKrwAmount;

    @Column(nullable = true, length = 32)
    private String paymentMethod;

    // 총 상품 금액(배송비를 제외)
    @Column(nullable = true, precision = 19, scale = 4)
    private BigDecimal totalProductAmount;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "draft", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<CheckoutDraftItem> items = new ArrayList<>();
}
