package com.sisibibi.api.domain.speech.repository;

import java.util.List;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Repository;

@Repository
public class RedisSpeakingQueueRepository {

    private static final DefaultRedisScript<Long> ASSIGN_SCRIPT =
            new DefaultRedisScript<>(
                    """
                    redis.call('ZREM', KEYS[1], ARGV[1])
                    redis.call('SET', KEYS[2], ARGV[1])
                    return 1
                    """,
                    Long.class
            );

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

    public void assign(Long roomId, Long userId) {
        redisTemplate.execute(
                ASSIGN_SCRIPT,
                List.of(queueKey(roomId), currentSpeakerKey(roomId)),
                userId.toString()
        );
    }

    private String queueKey(Long roomId) {
        return "stage:queue:{" + roomId + "}";
    }

    private String currentSpeakerKey(Long roomId) {
        return "stage:current:{" + roomId + "}";
    }
}
