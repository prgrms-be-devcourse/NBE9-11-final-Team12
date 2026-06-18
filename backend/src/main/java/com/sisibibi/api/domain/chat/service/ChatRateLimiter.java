package com.sisibibi.api.domain.chat.service;

import com.sisibibi.api.global.exception.CustomException;
import com.sisibibi.api.global.exception.ErrorCode;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatRateLimiter {

    private static final int LIMIT_COUNT = 5;
    private static final Duration LIMIT_WINDOW = Duration.ofSeconds(10);
    private static final String KEY_PREFIX = "chat:rate:";
    private static final String REDIS_ERROR_METRIC = "chat.rate_limiter.redis_error";

    private final StringRedisTemplate redisTemplate;
    private final MeterRegistry meterRegistry;

    public void check(Long userId) {
        String key = KEY_PREFIX + userId;

        try {
            Long count = redisTemplate.opsForValue().increment(key);
            if (count != null && count == 1L) {
                redisTemplate.expire(key, LIMIT_WINDOW);
            }

            if (count != null && count > LIMIT_COUNT) {
                throw new CustomException(ErrorCode.CHAT_RATE_LIMIT_EXCEEDED);
            }
        } catch (CustomException rateLimitException) {
            throw rateLimitException;
        } catch (RuntimeException redisException) {
            meterRegistry.counter(REDIS_ERROR_METRIC).increment();
            log.warn(
                    "Chat rate limiter Redis check failed. Allowing message. userId={}",
                    userId,
                    redisException
            );
        }
    }
}
