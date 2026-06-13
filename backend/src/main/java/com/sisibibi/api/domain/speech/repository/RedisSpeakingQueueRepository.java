package com.sisibibi.api.domain.speech.repository;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class RedisSpeakingQueueRepository {

    private final StringRedisTemplate redisTemplate;

    public RedisSpeakingQueueRepository(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void upsert(Long roomId, Long userId, int queueOrder) {
        redisTemplate.opsForZSet().add(
                queueKey(roomId),
                userId.toString(),
                queueOrder
        );
    }

    public void remove(Long roomId, Long userId) {
        redisTemplate.opsForZSet().remove(queueKey(roomId), userId.toString());
    }

    private String queueKey(Long roomId) {
        return "stage:queue:{" + roomId + "}";
    }
}
