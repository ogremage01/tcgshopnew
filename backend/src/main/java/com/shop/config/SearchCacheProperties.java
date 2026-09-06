package com.shop.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.cache.search-facets")
public class SearchCacheProperties {

    /**
     * true이면 Redis {@link org.springframework.cache.CacheManager}로 facet 캐시 사용. false면 캐시 비활성(NoOp).
     */
    private boolean enabled = false;

    /**
     * facet 캐시 TTL
     */
    private Duration ttl = Duration.ofSeconds(60);

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Duration getTtl() {
        return ttl;
    }

    public void setTtl(Duration ttl) {
        this.ttl = ttl;
    }
}
