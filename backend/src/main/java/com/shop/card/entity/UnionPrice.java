package com.shop.card.entity;

import java.math.BigDecimal;

import com.shop.common.util.UlidGenerator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "union_prices", indexes = {
                @Index(name = "idx_union_prices_check_code_refined", columnList = "check_code_refined"),
                @Index(name = "idx_union_prices_public_id", columnList = "public_id")
}, uniqueConstraints = {
                @UniqueConstraint(name = "uk_check_code_refined", columnNames = "check_code_refined")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UnionPrice {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        /** 게임 */
        private String game;
        /** Card/Sealed Product */
        private String productType;
        /** 소스별 print_type (FAB: Normal|Foil, TcgP: print_type, Mtg: type) */
        private String printType;
        /** 소스별 printing (FAB foil 변형명|Normal, TcgP: printing, Mtg: type) */
        private String printing;
        /** 가격 */
        private BigDecimal price;
        /** 세트 이름 */
        private String setName;
        /** 세트 코드 */
        private String setCode;
        /** 카드 이름 */
        private String cardName;
        /** 카드 이름(한글) */
        @Column(name = "card_namek")
        private String cardNameK;
        /** 레어도 */
        private String rarity;
        /** 체크 코드 리파인드(사람이 수동으로 수정한다) */
        @Column(name = "check_code_refined")
        private String checkCodeRefined;
        /** 이미지 소스. 허용값: OPB, TCGP, SCRYFALL */
        private String imageSource;
        /** 카드 이미지 URL1 */
        private String imageUrl;
        /** 양면 여부 */
        private Boolean isDoubleSided;
        /** UnionPrice Public ID */
        @Column(name = "public_id", nullable = false, unique = true, updatable = false, length = 26)
        private String publicId;

        /** 세트넘버(숫자만) */
        private Long setNumber;

        @PrePersist
        public void ensurePublicId() {
                if (publicId == null || publicId.isBlank()) {
                        publicId = UlidGenerator.nextUlid();
                }
        }
}
