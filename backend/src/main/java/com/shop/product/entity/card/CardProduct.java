package com.shop.product.entity.card;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.PrePersist;
import java.time.LocalDateTime;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import jakarta.persistence.Column;
import com.shop.common.util.UlidGenerator;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import com.shop.card.entity.UnionPrice;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

//싱글카드,밀봉제품
@Entity
@Table(name = "card_product")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class CardProduct {

    // 카드 제품 엔티티

    // 상품 ID
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    // 외부 노출용 ULID
    @Column(name = "public_id", nullable = false, unique = true, updatable = false, length = 26)
    private String publicId;
    // 제품 타입(Card/Sealed Product/supply?)
    private String productType;

    // 카드 상태
    @Column(name = "card_condition")
    private String condition;
    // 인쇄 형태(포일/노멀)
    private String printType;
    // 언어
    private String language;

    // 표시 여부
    private Boolean isVisible;
    // 삭제 여부
    private Boolean isDeleted;
    // 가격 오류(가격 연동 상품의 UnionPrice.price=0)로 자동 비공개 처리된 경우 true
    @Column(name = "hidden_by_price_error")
    private Boolean hiddenByPriceError;

    // 표시 재고
    private Long currentVisibleStock;
    // 최대 표시 수량
    private Long maxVisibleStock;
    // 총 수량
    private Long totalStock;
    // 자동 수량 업데이트 여부
    private Boolean isAutoUpdatedStock;

    // 저장소 ID
    private Long storageId;

    // 가격 연동 사용여부
    private Boolean isPriceLinked;
    // (가격 연동 시) 카드 가격 배율
    private Double pricingRate;
    // (가격 연동 하지 않을 시) 설정 가격
    private Long price;
    // 연동계산 된 가격
    private Long calculatedLinkedPrice;

    // 메모
    private String memo;

    // UnionPrice 연동
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "union_price_id")
    private UnionPrice unionPrice;

    // 생성 시간
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    // 수정 시간
    @LastModifiedDate
    @Column(nullable = false, updatable = true)
    private LocalDateTime updatedAt;

    @PrePersist
    public void ensurePublicId() {
        if (publicId == null || publicId.isBlank()) {
            publicId = UlidGenerator.nextUlid();
        }
    }

}
