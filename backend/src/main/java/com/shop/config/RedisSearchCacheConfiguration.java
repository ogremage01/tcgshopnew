package com.shop.config;

import java.time.Duration;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;

/**
 * facet 전용 캐시를 Redis에 둔다. 앱 기동 시 Redis는 필수이며, 미기동이면 캐시 접근 시점에 연결 실패할 수 있다.
 */
@Configuration
@ConditionalOnProperty(name = "app.cache.search-facets.enabled", havingValue = "true")
public class RedisSearchCacheConfiguration {

    @Bean
    @Primary
    public CacheManager redisSearchCacheManager(
            RedisConnectionFactory connectionFactory,
            SearchCacheProperties searchCacheProperties) {
        Duration ttl = searchCacheProperties.getTtl();
        RedisCacheConfiguration defaults = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(ttl != null ? ttl : Duration.ofSeconds(60))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(RedisSerializer.json()));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaults)
                .build();
    }
}
