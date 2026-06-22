package com.sisibibi.api.global.security.refresh;

import com.sisibibi.api.global.exception.CustomException;
import com.sisibibi.api.global.exception.ErrorCode;
import com.sisibibi.api.global.security.config.AuthProperties;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Component
public class RefreshTokenStore {

    private static final String ACTIVE_KEY_PREFIX = "auth:refresh:";
    private static final String USED_KEY_PREFIX = "auth:refresh-used:";
    private static final String USER_TOKEN_INDEX_PREFIX = "auth:refresh-index:";

    private final StringRedisTemplate redisTemplate;
    private final AuthProperties authProperties;

    public RefreshTokenStore(StringRedisTemplate redisTemplate, AuthProperties authProperties) {
        this.redisTemplate = redisTemplate;
        this.authProperties = authProperties;
    }

    public void save(Long userId, String tokenId, String refreshToken) {
        redisTemplate.opsForValue().set(
                activeKey(userId, tokenId),
                hash(refreshToken),
                authProperties.jwt().refreshTokenExpiration()
        );
        redisTemplate.opsForSet().add(indexKey(userId), tokenId);
        redisTemplate.expire(indexKey(userId), authProperties.jwt().refreshTokenExpiration());
    }

    public void verifyAndDelete(Long userId, String tokenId, String refreshToken) {
        String activeKey = activeKey(userId, tokenId);
        String storedHash = redisTemplate.opsForValue().get(activeKey);

        if (storedHash == null) {
            if (Boolean.TRUE.equals(redisTemplate.hasKey(usedKey(userId, tokenId)))) {
                throw new CustomException(ErrorCode.REFRESH_TOKEN_REUSED);
            }
            throw new CustomException(ErrorCode.REFRESH_TOKEN_NOT_FOUND);
        }

        if (!Objects.equals(storedHash, hash(refreshToken))) {
            redisTemplate.delete(activeKey);
            redisTemplate.opsForValue().set(
                    usedKey(userId, tokenId),
                    "1",
                    usedTokenRetention()
            );
            throw new CustomException(ErrorCode.REFRESH_TOKEN_REUSED);
        }

        redisTemplate.delete(activeKey);
        redisTemplate.opsForSet().remove(indexKey(userId), tokenId);
        redisTemplate.opsForValue().set(
                usedKey(userId, tokenId),
                "1",
                usedTokenRetention()
        );
    }

    public void delete(Long userId, String tokenId) {
        redisTemplate.delete(activeKey(userId, tokenId));
        redisTemplate.opsForSet().remove(indexKey(userId), tokenId);
    }

    public void deleteAll(Long userId) {
        Set<String> tokenIds = redisTemplate.opsForSet().members(indexKey(userId));
        if (tokenIds == null || tokenIds.isEmpty()) {
            redisTemplate.delete(indexKey(userId));
            return;
        }

        List<String> activeKeys = tokenIds.stream()
                .map(tokenId -> activeKey(userId, tokenId))
                .toList();
        redisTemplate.delete(activeKeys);
        redisTemplate.delete(indexKey(userId));
    }

    String activeKey(Long userId, String tokenId) {
        return ACTIVE_KEY_PREFIX + userId + ":" + tokenId;
    }

    String usedKey(Long userId, String tokenId) {
        return USED_KEY_PREFIX + userId + ":" + tokenId;
    }

    String indexKey(Long userId) {
        return USER_TOKEN_INDEX_PREFIX + userId;
    }

    private Duration usedTokenRetention() {
        return authProperties.jwt()
                .refreshTokenExpiration()
                .plus(authProperties.jwt().accessTokenExpiration());
    }

    private String hash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hashed);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm is not available.", e);
        }
    }
}
