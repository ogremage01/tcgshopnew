package com.shop.product.entity.sealedProduct;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.shop.common.util.UlidGenerator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "sealed_product")
@Getter
@Setter
public class SealedProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String productNameEn;

    private String productNameKo;

    private String description;

    private String imageUrl;

    private Long price;

    private Integer currentVisibleStock;

    private Integer maxVisibleStock;

    private Integer totalStock;

    private Boolean isActive;

    private Boolean isDeleted;

    /** 게임 분류 (예: "Magic: The Gathering") — 밀봉은 ProductIp FK 없이 game으로 구분 */
    private String game;

    private String setName;

    private String setCode;

    private String language; // 예: "EN", "KO"

    @Column(name = "public_id", nullable = false, unique = true, updatable = false, length = 26)
    private String publicId;

    @CreatedDate
    private LocalDateTime createdAt;

    
    @LastModifiedDate
    private LocalDateTime updatedAt;

    /** 오프라인 상품 고유 ID. id가 아니라 productId */
    private String offlineProductId;

    /** 고객 표시·주문 가능 재고 */
    public long visibleStock() {
        return currentVisibleStock != null ? currentVisibleStock.longValue() : 0L;
    }


    @PrePersist
    public void ensurePublicId() {
        if (publicId == null || publicId.isBlank()) {
            publicId = UlidGenerator.nextUlid();
        }
    }

}
