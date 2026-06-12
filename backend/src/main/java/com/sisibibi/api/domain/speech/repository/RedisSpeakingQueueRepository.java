package com.sisibibi.api.domain.speech.repository;

import java.util.List;
import java.util.OptionalInt;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Repository;

@Repository
public class RedisSpeakingQueueRepository {

    private static final long DUPLICATE_REQUEST = -1L;
    private static final DefaultRedisScript<Long> ENQUEUE_SCRIPT = createEnqueueScript();

    private final StringRedisTemplate redisTemplate;

    public RedisSpeakingQueueRepository(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public OptionalInt enqueue(Long roomId, Long userId) {
        Long result = redisTemplate.execute(
                ENQUEUE_SCRIPT,
                List.of(queueKey(roomId), sequenceKey(roomId)),
                userId.toString()
        );

        if (result == null) {
            throw new IllegalStateException("Redis did not return a queue order.");
        }

        if (result == DUPLICATE_REQUEST) {
            return OptionalInt.empty();
        }

        return OptionalInt.of(Math.toIntExact(result));
    }

    public void remove(Long roomId, Long userId) {
        redisTemplate.opsForZSet().remove(queueKey(roomId), userId.toString());
    }

    private static DefaultRedisScript<Long> createEnqueueScript() {
        String script = """
                if redis.call('ZSCORE', KEYS[1], ARGV[1]) then
                    return -1
                end

                local sequence = redis.call('INCR', KEYS[2])
                redis.call('ZADD', KEYS[1], sequence, ARGV[1])
                return sequence
                """;

        return new DefaultRedisScript<>(script, Long.class);
    }

    private String queueKey(Long roomId) {
        return "stage:queue:{" + roomId + "}";
    }

    private String sequenceKey(Long roomId) {
        return "stage:queue:{" + roomId + "}:sequence";
    }
}
