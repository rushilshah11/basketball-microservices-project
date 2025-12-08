package com.bball.stats.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableCaching
public class RedisCacheConfig {

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        // Default configuration with JSON serialization
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer()))
                .disableCachingNullValues();

        // Different TTLs for different types of data
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();

        // Player basic info - cache for 24 hours (doesn't change often)
        cacheConfigurations.put("player_info", defaultConfig
                .entryTtl(Duration.ofHours(24)));

        // Player stats - cache for 6 hours (updates after games)
        cacheConfigurations.put("player_stats", defaultConfig
                .entryTtl(Duration.ofHours(6)));

        // Game logs - cache for 1 hour (more dynamic)
        cacheConfigurations.put("game_logs", defaultConfig
                .entryTtl(Duration.ofHours(1)));

        // Trending players - cache for 12 hours
        cacheConfigurations.put("trending_players", defaultConfig
                .entryTtl(Duration.ofHours(12)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .build();
    }
}


