package com.sisibibi.api.global.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

@Slf4j
@Configuration
@EnableCaching
public class RedisCacheConfig implements CachingConfigurer {

  @Bean
  public CacheManager cacheManager(
      RedisConnectionFactory redisConnectionFactory,
      ObjectMapper objectMapper
  ) {
    GenericJackson2JsonRedisSerializer serializer =
        new GenericJackson2JsonRedisSerializer(objectMapper);

    RedisSerializationContext.SerializationPair<Object> valueSerializer =
        RedisSerializationContext.SerializationPair.fromSerializer(serializer);

    RedisCacheConfiguration defaultCacheConfiguration = RedisCacheConfiguration
        .defaultCacheConfig()
        .entryTtl(Duration.ofMinutes(10))
        .disableCachingNullValues()
        .serializeValuesWith(valueSerializer);

    RedisCacheConfiguration topicCandidateCacheConfiguration = RedisCacheConfiguration
        .defaultCacheConfig()
        .entryTtl(Duration.ofHours(3))
        .disableCachingNullValues()
        .serializeValuesWith(valueSerializer);

    return RedisCacheManager.builder(redisConnectionFactory)
        .cacheDefaults(defaultCacheConfiguration)
        .withInitialCacheConfigurations(Map.of(
            "topicCandidates", topicCandidateCacheConfiguration
        ))
        .build();
  }

  @Override
  public CacheErrorHandler errorHandler() {
    return new CacheErrorHandler() {
      @Override
      public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
        log.warn(
            "Redis cache get failed. cacheName={}, key={}",
            cache.getName(),
            key,
            exception
        );
      }

      @Override
      public void handleCachePutError(
          RuntimeException exception,
          Cache cache,
          Object key,
          Object value
      ) {
        log.warn(
            "Redis cache put failed. cacheName={}, key={}",
            cache.getName(),
            key,
            exception
        );
      }

      @Override
      public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
        log.warn(
            "Redis cache evict failed. cacheName={}, key={}",
            cache.getName(),
            key,
            exception
        );
      }

      @Override
      public void handleCacheClearError(RuntimeException exception, Cache cache) {
        log.warn(
            "Redis cache clear failed. cacheName={}",
            cache.getName(),
            exception
        );
      }
    };
  }
}