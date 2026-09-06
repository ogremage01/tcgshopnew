package com.shop.order.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.shop.order.entity.OrderConfig;

public interface OrderConfigRepository extends JpaRepository<OrderConfig, Long> {

    Optional<OrderConfig> findByConfigKey(String configKey);

    @Modifying
    @Query("UPDATE OrderConfig SET configValue = :configValue WHERE configKey = :configKey")
    void updateConfigValue(@Param("configKey") String configKey, @Param("configValue") String configValue);

}
