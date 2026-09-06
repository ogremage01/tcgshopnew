package com.shop.product.entity.manualProduct;

import jakarta.persistence.Entity;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Column;
import java.time.LocalDateTime;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import com.shop.common.util.UlidGenerator;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import jakarta.persistence.EntityListeners;

@Entity
@Table(name = "manual_products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class ManualProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String nameEn;
    private String nameKo;
    @Column(columnDefinition = "TEXT")
    private String description;
    private Long price;
    private Long stock;
    /** ProductCategory 마스터에서 선택한 이름(영문). FK가 아니라 표시·검색용 값 */
    private String productType;
    /** ProductIp 마스터에서 선택한 이름(영문). FK가 아니라 표시·검색용 값 */
    private String productIp;

    private String imgUrl;

    private Boolean isDeleted;
    private Boolean isVisible;
    private String publicId;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    @LastModifiedDate
    @Column(nullable = false, updatable = true)
    private LocalDateTime updatedAt;

    /** 오프라인 상품 고유 ID. id가 아니라 productId */
    private String offlineProductId;

    @PrePersist
    public void ensurePublicId() {
        if (publicId == null || publicId.isBlank()) {
            publicId = UlidGenerator.nextUlid();
        }
    }
}
