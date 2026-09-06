package com.shop.card.entity;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Builder
@Entity
/*
 * UPSERT(INSERT ... ON DUPLICATE KEY UPDATE)가 동작하려면
 * DB에 UNIQUE KEY가 있어야 한다. Hibernate가 ddl-auto: update 시
 * 아래 제약을 자동으로 생성한다.
 *
 * ※ 주의: 기존 데이터에 (set_code, code, type) 조합이 중복된 행이 있으면
 * 애플리케이션 기동 시 인덱스 생성이 실패한다.
 * 적용 전 중복 데이터를 정리해야 한다.
 */
@Table(name = "mtg_prices", uniqueConstraints = {
                @UniqueConstraint(name = "uq_mtg_prices_set_code_type", columnNames = { "set_code", "code", "type" })
}, indexes = {
                @Index(name = "idx_mtg_prices_check_code_refined", columnList = "check_code_refined")
})
@AllArgsConstructor
public class MtgPrice {

        // mtg-kr에서 받아오는 mtg 카드 가격 정보 엔티티

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;
        @Column(name = "set_code")
        private String set;
        private String code;
        private String type;
        private BigDecimal price;
        @JsonProperty("setname")
        private String setName;
        private String name;
        @JsonProperty("name_k")
        @Column(name = "name_k")
        private String nameK;
        @Column(name = "tcg_p_price_id")
        private Long tcgPPriceId;
        private String rarity;
        @Column(name = "is_double_sided")
        private Boolean isDoubleSided;
        private String layout;
        private Boolean downloaded;

        // 체크 코드
        private String checkCode;
        // 체크 코드 리파인드(사람이 수동으로 수정한다)
        private String checkCodeRefined;
        private String conNumName;
}
