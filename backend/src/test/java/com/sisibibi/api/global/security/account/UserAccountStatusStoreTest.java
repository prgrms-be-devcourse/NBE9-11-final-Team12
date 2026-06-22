package com.sisibibi.api.global.security.account;

import com.sisibibi.api.domain.user.entity.User;
import com.sisibibi.api.domain.user.repository.UserRepository;
import com.sisibibi.api.global.security.config.AuthProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserAccountStatusStoreTest {

    private final StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
    private final ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private UserAccountStatusStore userAccountStatusStore;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        userAccountStatusStore = new UserAccountStatusStore(
                redisTemplate,
                userRepository,
                authProperties()
        );
    }

    @Test
    void isBanned_returnsTrueWithoutDbLookup_whenBannedCacheExists() {
        when(valueOperations.get("auth:user-status:10")).thenReturn("BANNED");

        assertThat(userAccountStatusStore.isBanned(10L)).isTrue();

        verify(userRepository, never()).findById(10L);
    }

    @Test
    void isBanned_checksDb_whenLegacyActiveCacheExists() {
        User user = User.signup("user@example.com", "password", "user");
        user.ban();
        when(valueOperations.get("auth:user-status:10")).thenReturn("ACTIVE");
        when(userRepository.findById(10L)).thenReturn(Optional.of(user));

        assertThat(userAccountStatusStore.isBanned(10L)).isTrue();
    }

    @Test
    void isBanned_doesNotCacheActiveStatus() {
        User user = User.signup("user@example.com", "password", "user");
        when(valueOperations.get("auth:user-status:10")).thenReturn(null);
        when(userRepository.findById(10L)).thenReturn(Optional.of(user));

        assertThat(userAccountStatusStore.isBanned(10L)).isFalse();

        verify(valueOperations, never()).set(
                "auth:user-status:10",
                "ACTIVE",
                Duration.ofSeconds(30)
        );
    }

    @Test
    void markActive_evictsBannedCache() {
        userAccountStatusStore.markActive(10L);

        verify(redisTemplate).delete("auth:user-status:10");
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
