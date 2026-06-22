package com.sisibibi.api.global.security;

import com.sisibibi.api.global.exception.CustomException;
import com.sisibibi.api.global.exception.ErrorCode;
import com.sisibibi.api.global.security.config.AuthProperties;
import com.sisibibi.api.global.security.refresh.RefreshTokenStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.LinkedHashSet;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RefreshTokenStoreTest {

    private final StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
    private final ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
    private final SetOperations<String, String> setOperations = mock(SetOperations.class);
    private RefreshTokenStore refreshTokenStore;

    @BeforeEach
    void setUp() {
        refreshTokenStore = new RefreshTokenStore(redisTemplate, authProperties());
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
    }

    @Test
    void save_storesHashedTokenWithRefreshTtl() {
        refreshTokenStore.save(1L, "token-id", "refresh-token");

        verify(valueOperations).set(
                eq("auth:refresh:1:token-id"),
                anyString(),
                eq(Duration.ofDays(14))
        );
        verify(setOperations).add("auth:refresh-index:1", "token-id");
    }

    @Test
    void verifyAndDelete_deletesActiveTokenAndMarksUsedToken() {
        refreshTokenStore.save(1L, "token-id", "refresh-token");
        String storedHash = captureStoredHash();
        when(valueOperations.get("auth:refresh:1:token-id")).thenReturn(storedHash);

        refreshTokenStore.verifyAndDelete(1L, "token-id", "refresh-token");

        verify(redisTemplate).delete("auth:refresh:1:token-id");
        verify(setOperations).remove("auth:refresh-index:1", "token-id");
        verify(valueOperations).set(
                "auth:refresh-used:1:token-id",
                "1",
                Duration.ofDays(14).plusMinutes(30)
        );
    }

    @Test
    void deleteAll_deletesEveryIndexedRefreshToken() {
        when(setOperations.members("auth:refresh-index:1"))
                .thenReturn(new LinkedHashSet<>(java.util.List.of("token-1", "token-2")));

        refreshTokenStore.deleteAll(1L);

        verify(redisTemplate).delete(java.util.List.of(
                "auth:refresh:1:token-1",
                "auth:refresh:1:token-2"
        ));
        verify(redisTemplate).delete("auth:refresh-index:1");
    }

    @Test
    void verifyAndDelete_rejectsMissingRefreshToken() {
        when(valueOperations.get("auth:refresh:1:token-id")).thenReturn(null);
        when(redisTemplate.hasKey("auth:refresh-used:1:token-id")).thenReturn(false);

        assertThatThrownBy(() -> refreshTokenStore.verifyAndDelete(1L, "token-id", "refresh-token"))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.REFRESH_TOKEN_NOT_FOUND);
    }

    @Test
    void verifyAndDelete_rejectsReusedRefreshToken() {
        when(valueOperations.get("auth:refresh:1:token-id")).thenReturn(null);
        when(redisTemplate.hasKey("auth:refresh-used:1:token-id")).thenReturn(true);

        assertThatThrownBy(() -> refreshTokenStore.verifyAndDelete(1L, "token-id", "refresh-token"))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.REFRESH_TOKEN_REUSED);
    }

    private String captureStoredHash() {
        String key = "auth:refresh:1:token-id";
        refreshTokenStore.save(1L, "token-id", "refresh-token");
        return org.mockito.Mockito.mockingDetails(valueOperations)
                .getInvocations()
                .stream()
                .filter(invocation -> invocation.getMethod().getName().equals("set"))
                .filter(invocation -> key.equals(invocation.getArgument(0)))
                .reduce((first, second) -> second)
                .map(invocation -> invocation.<String>getArgument(1))
                .orElseThrow();
    }

    private AuthProperties authProperties() {
        return new AuthProperties(
                new AuthProperties.Jwt(
                        "test-jwt-secret-key-must-be-at-least-32-bytes-long",
                        Duration.ofMinutes(30),
                        Duration.ofDays(14)
                ),
                new AuthProperties.Cookie(false, "Lax", null)
        );
    }
}
