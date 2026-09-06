package com.shop.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.support.NoOpCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
@ConditionalOnProperty(name = "app.cache.search-facets.enabled", havingValue = "false", matchIfMissing = true)
public class NoopSearchCacheConfiguration {

    @Bean
    @Primary
    public CacheManager noopCacheManager() {
        return new NoOpCacheManager();
    }
}
