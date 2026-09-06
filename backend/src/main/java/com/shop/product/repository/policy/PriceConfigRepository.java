package com.shop.product.repository.policy;

import org.springframework.data.jpa.repository.JpaRepository;

import com.shop.product.entity.card.policy.PriceConfig;
import com.shop.product.entity.card.policy.PriceConfigId;

import java.util.List;
import java.util.Optional;

public interface PriceConfigRepository extends JpaRepository<PriceConfig, PriceConfigId> {

    /**
     * 
     * @param configKey
     * @param configGame
     * @return
     */
    Optional<PriceConfig> findByConfigKeyAndConfigGame(String configKey, String configGame);

    /**
     * 
     * @param configKey
     * @return
     */
    List<PriceConfig> findByConfigKey(String configKey);

}
