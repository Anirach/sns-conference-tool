package com.sns.app.config;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis-backed cache with short TTL for hot read paths (vicinity). Invalidation is driven by
 * domain events: {@code LocationUpdated} and {@code MatchRecomputeRequested} evict the
 * event-scoped slice of the {@code vicinity} cache.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public RedisCacheManager cacheManager(
        RedisConnectionFactory cf,
        @Value("${sns.cache.vicinity-ttl-seconds:10}") long vicinityTtl
    ) {
        RedisCacheConfiguration base = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(5))
            .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()))
            .disableCachingNullValues();
        return RedisCacheManager.builder(cf)
            .cacheDefaults(base)
            .withCacheConfiguration("vicinity",
                base.entryTtl(Duration.ofSeconds(vicinityTtl)))
            .build();
    }
}
