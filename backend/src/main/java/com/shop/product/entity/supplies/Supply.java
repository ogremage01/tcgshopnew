package com.shop.product.entity.supplies;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Column;
import jakarta.persistence.PrePersist;
import java.time.LocalDateTime;

import com.shop.common.util.UlidGenerator;

@Entity
@Table(name = "supplies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class Supply {

    // 서플라이 엔티티

    // 서플라이 ID
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    // 외부 노출용 ULID
    @Column(name = "public_id", nullable = false, unique = true, updatable = false, length = 26)
    private String publicId;
    // 서플라이 이름
    private String nameEn;
    // 서플라이 이름(한글)
    private String nameKo;
    // 서플라이 설명 (HTML 리치 텍스트)
    @Column(columnDefinition = "TEXT")
    private String description;
    // 서플라이 가격
    private Long price;
    // 서플라이 재고
    private Long stock;

    // 서플라이 타입
    private String supplyType;

    // 제조사
    private String maker;

    // 공개 여부
    private Boolean isVisible;

    // 삭제 여부
    private Boolean isDeleted;

    // 이미지 URL
    private String imgUrl;

    // 서플라이 생성 시간
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    // 서플라이 수정 시간
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
