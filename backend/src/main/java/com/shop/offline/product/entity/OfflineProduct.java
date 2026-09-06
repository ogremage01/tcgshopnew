package com.shop.offline.product.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
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
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "offline_products", indexes = {
    @Index(name = "idx_offline_products_title_link_table", columnList = "title,link_table_name")
})
public class OfflineProduct {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String productId;
    private String categoryId;
    private String categoryTitle;
    private String title;
    private Integer priceUnit;
    private Integer priceValue;
    private String barcode;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    //--------------------------------
    // 링크 테이블 관련 필드(연결 테이블 조회 시 사용. 토스에서 받아오는 데이터 아님)
    private String linkTableName;
    private Long linkId;
    /**
     * 입고 수량
     */
    private Integer receivingQuantity;
    /**
     * 출고 수량
     */
    private Integer shippingQuantity;
}
