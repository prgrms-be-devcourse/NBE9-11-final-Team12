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
            if (UserStatus.BANNED.name().equals(cachedStatus)) {
                return true;
            }
        } catch (RuntimeException redisException) {
            log.warn("Failed to read user status cache. userId={}", userId, redisException);
        }

        UserStatus status = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND))
                .getStatus();
        if (status == UserStatus.BANNED) {
            cacheBanned(userId);
        }
        return status == UserStatus.BANNED;
    }

    public void markBanned(Long userId) {
        cacheBanned(userId);
    }

    public void markActive(Long userId) {
        try {
            redisTemplate.delete(key(userId));
        } catch (RuntimeException redisException) {
            log.warn("Failed to evict user status cache. userId={}", userId, redisException);
        }
    }

    private void cacheBanned(Long userId) {
        try {
            redisTemplate.opsForValue().set(
                    key(userId),
                    UserStatus.BANNED.name(),
                    authProperties.jwt().accessTokenExpiration()
            );
        } catch (RuntimeException redisException) {
            log.warn(
                    "Failed to update banned user cache. userId={}",
                    userId,
                    redisException
            );
        }
    }

    private String key(Long userId) {
        return KEY_PREFIX + userId;
    }
}
