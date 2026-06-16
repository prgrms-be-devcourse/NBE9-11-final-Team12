package com.sisibibi.api.domain.chat.service;

import com.sisibibi.api.global.exception.CustomException;
import com.sisibibi.api.global.exception.ErrorCode;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ChatRateLimiterTest {

    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> valueOperations;
    private SimpleMeterRegistry meterRegistry;
    private ChatRateLimiter rateLimiter;

    @BeforeEach
    void setUp() {
        redisTemplate = mock(StringRedisTemplate.class);
        valueOperations = mock(ValueOperations.class);
        meterRegistry = new SimpleMeterRegistry();
        rateLimiter = new ChatRateLimiter(redisTemplate, meterRegistry);
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
    }

    @Test
    void check_allowsMessageAndSetsTtl_whenFirstMessageInWindow() {
        given(valueOperations.increment("chat:rate:1")).willReturn(1L);

        rateLimiter.check(1L);

        verify(redisTemplate).expire("chat:rate:1", Duration.ofSeconds(10));
    }

    @Test
    void check_throwsRateLimitExceeded_whenCountIsGreaterThanLimit() {
        given(valueOperations.increment("chat:rate:1")).willReturn(6L);

        assertThatThrownBy(() -> rateLimiter.check(1L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.CHAT_RATE_LIMIT_EXCEEDED);
    }

    @Test
    void check_failsOpenAndRecordsMetric_whenRedisFails() {
        given(valueOperations.increment("chat:rate:1")).willThrow(new RuntimeException("redis down"));

        rateLimiter.check(1L);

        assertThat(meterRegistry.counter("chat.rate_limiter.redis_error").count()).isEqualTo(1.0);
    }
}
