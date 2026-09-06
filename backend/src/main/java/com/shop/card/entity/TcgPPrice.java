package com.shop.card.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.hibernate.annotations.DynamicUpdate;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import jakarta.persistence.Column;
import java.math.BigDecimal;
import jakarta.persistence.Index;

@Entity
@DynamicUpdate
/*
 * UPSERT(INSERT ... ON DUPLICATE KEY UPDATE)가 동작하려면
 * DB에 UNIQUE KEY가 있어야 한다. Hibernate가 ddl-auto: update 시
 * 아래 제약을 자동으로 생성한다.
 *
 * card_condition: @Column(name = "card_condition")로 매핑된 컬럼명 사용
 *
 * ※ 주의: 기존 데이터에 (product_id, card_condition, printing, product_name) 조합이 중복된 행이 있으면
 * 애플리케이션 기동 시 인덱스 생성이 실패한다.
 * 적용 전 중복 데이터를 정리해야 한다.
 */
@Table(name = "tcg_p_prices", uniqueConstraints = {
                @UniqueConstraint(name = "uq_tcg_p_prices_product_condition_printing", columnNames = { "product_id",
                                "card_condition", "printing", "product_name" })
}, indexes = {
                @Index(name = "idx_tcg_p_prices_check_code", columnList = "check_code"),
                @Index(name = "idx_tcg_p_prices_check_code_refined", columnList = "check_code_refined")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TcgPPrice {

        // TCGPrice 엔티티

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        /** 외부 API는 productID(대문자 ID) 키를 쓰므로 명시 매핑한다. */
        @JsonProperty("productID")
        private Long productId;
        @Column(name = "card_condition")
        private String condition;
        // 게임 라인 ID
        private String game;
        // 미사용
        private Boolean isSupplemental;
        // 시장 가격
        private BigDecimal marketPrice;
        // 넘버
        private String number;
        // 프린트 타입
        private String printing;
        // 상품 이름
        private String productName;
        // 레어도
        private String rarity;
        // 세트 이름
        @Column(name = "set_name")
        private String set;
        // 세트 약어
        private String setAbbrv;
        // 상품 타입(Card/Sealed Product)
        private String type;
        // -------------------------------------------------------------------------------------------------
        // 이 아래로는 외부 API에서 온 데이터가 아니므로 직접 저장한다.
        // -------------------------------------------------------------------------------------------------

        // 프린트 타입(포일/노멀)
        private String printType;
        // 양면카드 여부
        private Boolean isDoubleSided;
        // 이미지 다운로드 여부(false: 다운로드 안됨, true: 다운로드 됨)
        private Boolean downloaded;
        // 코드 넘버(세트-넘버)
        private String codeNumber;
        // 체크 코드
        private String checkCode;
        /**
         * 중복 검사 후 확정된 매칭 키. fab_prices·mtg_prices의 check_code_refined와 직접 비교한다.
         * 배치 내 중복 발생 시 NULL로 처리되어 overlay JOIN에서 자동 제외된다.
         */
        @Column(name = "check_code_refined")
        private String checkCodeRefined;
        /** Open Binder(mtg_prices / fab_prices) 매칭 행의 PK. 게임에 따라 한쪽 테이블만 의미 있다. */
        @Column(name = "ob_price_id")
        private Long obPriceId;
}
