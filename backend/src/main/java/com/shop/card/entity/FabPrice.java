package com.shop.card.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import jakarta.persistence.Column;
import jakarta.persistence.Index;

@Entity
/*
 * UPSERT(INSERT ... ON DUPLICATE KEY UPDATE)가 동작하려면
 * DB에 UNIQUE KEY가 있어야 한다. Hibernate가 ddl-auto: update 시
 * 아래 제약을 자동으로 생성한다.
 *
 * ※ 주의: 기존 데이터에 (set_code, code) 조합이 중복된 행이 있으면
 * 애플리케이션 기동 시 인덱스 생성이 실패한다.
 * 적용 전 중복 데이터를 정리해야 한다.
 */
@Table(name = "fab_prices", uniqueConstraints = {
                @UniqueConstraint(name = "uq_fab_prices_set_code", columnNames = { "set_code", "code" })
}, indexes = {
                @Index(name = "idx_fab_prices_check_code_refined", columnList = "check_code_refined")
})
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class FabPrice {

        // mtg-kr에서 받아오는 fab 가격 정보 엔티티

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @Column(name = "set_code")
        private String set;

        private String code;

        private BigDecimal price;

        private String rarity;

        @JsonProperty("setname")
        private String setName;

        @JsonProperty("cardname")
        private String cardName;

        @JsonProperty("collector_num")
        private String collectorNum;

        private String foil;
        private Boolean downloaded;
        // ----------------------------------------------------
        // 외부 API에서 온 데이터가 아니므로 직접 저장한다.
        // ----------------------------------------------------
        /** TCGPrice의 id */
        @Column(name = "tcg_p_price_id")
        private Long tcgPPriceId;

        private String checkCode;
        private String checkCodeRefined;
        private String conNumName;
}
