package com.shop.search.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.shop.search.dto.enums.ProductTableEnum;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
@Entity
    @Table(name = "product_search_maps", indexes = {
        @Index(name = "idx_product_search_maps_table_id_product_id", columnList = "table_name,product_id"),
        @Index(name = "idx_product_search_maps_table_name_source_id", columnList = "table_name,source_id"),
        @Index(name = "idx_product_search_maps_table_catalog_source_id", columnList = "table_name,catalog_source_id"),
        @Index(name = "idx_product_search_maps_table_name_source_public_id", columnList = "table_name,source_public_id"),
        @Index(name = "idx_product_search_maps_table_game_set_code", columnList = "table_name,game,set_code"),
        @Index(name = "idx_product_search_maps_visible_table", columnList = "is_visible,table_name") })
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class ProductSearchMap {

    // 상품 검색 매핑 엔티티

    // 상품 검색 매핑 ID
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 상품 이름(영어) - 검색용
    private String productName;
    // 상품 이름(한글) - 검색용
    private String productNameKo;
    // 상품 게임(MTG/FAB 외 게임 라인) - 필터용
    private String game;
    /** UnionPrice.setCode — 세트 브라우즈·필터용(비정규화) */
    @Column(name = "set_code", length = 64)
    private String setCode;
    // 상품 타입(Card/Sealed Product/supply?) - 필터용
    private String productType;
    // supplies 상품 타입 - 필터용
    private String suppliesType;
    /** 수동 상품 카테고리(preorder, event-ticket 등) — MANUAL_PRODUCT 행만 사용 */
    @Column(name = "manual_category", length = 64)
    private String manualCategory;
    // 포일 여부 - 필터용
    private String printType;
    // 재고 유무 - 필터용
    private Boolean inStock;

    // 상품 ID(card_product.public_id) - SKU 단위 식별용. public_id 참조
    @Column(name = "product_id", nullable = false, unique = true, updatable = false, length = 26)
    private String productId;

    // 테이블 이름(소속테이블명) - 필터용(인덱스)
    @Enumerated(EnumType.STRING)
    @Column(name = "table_name" , length = 50)
    private ProductTableEnum tableName;
    // 소속 테이블의 레코드 ID - 검색용(인덱스)
    private Long sourceId;
    @Column(name = "catalog_source_id")
    private Long catalogSourceId;
    // 소속 테이블의 공개 식별자(ULID) - 외부 노출 전환 대비
    @Column(name = "source_public_id", length = 26)
    private String sourcePublicId;

    // 기준가. UnionPrice.price, Supplies.price - 정렬용
    private BigDecimal sortPrice;

    // 표시 여부
    private Boolean isVisible;

    // 갱신 시간 - 디버그용
    @LastModifiedDate
    private LocalDateTime updatedAt;
}
