package com.shop.product.entity.card.policy;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "price_config")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
@IdClass(PriceConfigId.class)
public class PriceConfig {

    /**
     * 
     * @example "minimum_price"
     * @example "us_currency_rate"
     */

    @Id
    @Column(nullable = false)
    private String configKey;
    /**
     * 
     * @example "Magic: The Gathering"
     * @example "Flesh & Blood TCG"
     * @example "Star Wars Unlimited"
     * @example "Riftbound League of Legends Trading Card Game"
     */
    @Id
    @Column(nullable = false)
    private String configGame;
    // 19자리 10진수, 소수점 4자리
    @Column(nullable = false)
    private BigDecimal configValue;

    @Column(nullable = false, updatable = true)
    @LastModifiedDate
    private LocalDateTime updatedAt;
}
