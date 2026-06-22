package com.sisibibi.api.global.security.account;

import com.sisibibi.api.domain.user.entity.UserStatus;
import com.sisibibi.api.domain.user.repository.UserRepository;
import com.sisibibi.api.global.exception.CustomException;
import com.sisibibi.api.global.exception.ErrorCode;
import com.sisibibi.api.global.security.config.AuthProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserAccountStatusStore {

    private static final String KEY_PREFIX = "auth:user-status:";

    private final StringRedisTemplate redisTemplate;
    private final UserRepository userRepository;
    private final AuthProperties authProperties;

    public boolean isBanned(Long userId) {
        try {
            String cachedStatus = redisTemplate.opsForValue().get(key(userId));
            if (cachedStatus != null) {
                return UserStatus.BANNED.name().equals(cachedStatus);
            }
        } catch (RuntimeException redisException) {
            log.warn("Failed to read user status cache. userId={}", userId, redisException);
        }

        UserStatus status = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND))
                .getStatus();
        cache(userId, status);
        return status == UserStatus.BANNED;
    }

    public void markBanned(Long userId) {
        cache(userId, UserStatus.BANNED);
    }

    public void markActive(Long userId) {
        cache(userId, UserStatus.ACTIVE);
    }

    private void cache(Long userId, UserStatus status) {
        try {
            redisTemplate.opsForValue().set(
                    key(userId),
                    status.name(),
                    cacheDuration(status)
            );
        } catch (RuntimeException redisException) {
            log.warn(
                    "Failed to update user status cache. userId={}, status={}",
                    userId,
                    status,
                    redisException
            );
        }
    }

    private String key(Long userId) {
        return KEY_PREFIX + userId;
    }

    private Duration cacheDuration(UserStatus status) {
        if (status == UserStatus.BANNED) {
            return authProperties.jwt().accessTokenExpiration();
        }
        return Duration.ofSeconds(30);
    }
}
