package com.sisibibi.api.global.websocket;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Repository;

@Repository
public class RedisRoomPresenceRepository {

    private static final String STATUS_CONNECTED = "CONNECTED";
    private static final String STATUS_DISCONNECTED = "DISCONNECTED";

    private static final DefaultRedisScript<Long> MARK_CONNECTED_SCRIPT =
            new DefaultRedisScript<>(
                    """
                    local generation = redis.call('HINCRBY', KEYS[1], 'generation', 1)
                    redis.call(
                        'HSET',
                        KEYS[1],
                        'status', 'CONNECTED',
                        'sessionId', ARGV[1],
                        'connectedAt', ARGV[2],
                        'disconnectedAt', '',
                        'expiresAt', ''
                    )
                    return generation
                    """,
                    Long.class
            );

    private static final DefaultRedisScript<Long> MARK_DISCONNECTED_SCRIPT =
            new DefaultRedisScript<>(
                    """
                    local currentSessionId = redis.call('HGET', KEYS[1], 'sessionId')
                    if currentSessionId ~= ARGV[1] then
                        return 0
                    end

                    local generation = redis.call('HGET', KEYS[1], 'generation')
                    if generation == false then
                        generation = redis.call('HINCRBY', KEYS[1], 'generation', 1)
                    end

                    redis.call(
                        'HSET',
                        KEYS[1],
                        'status', 'DISCONNECTED',
                        'disconnectedAt', ARGV[2],
                        'expiresAt', ARGV[3]
                    )
                    redis.call('ZADD', KEYS[2], ARGV[3], ARGV[4] .. ':' .. generation)
                    return generation
                    """,
                    Long.class
            );

    private final StringRedisTemplate redisTemplate;

    public RedisRoomPresenceRepository(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public long markConnected(
            Long roomId,
            Long userId,
            String sessionId,
            Instant connectedAt
    ) {
        Long generation = redisTemplate.execute(
                MARK_CONNECTED_SCRIPT,
                List.of(presenceKey(roomId, userId)),
                sessionId,
                String.valueOf(connectedAt.toEpochMilli())
        );
        return generation == null ? 0L : generation;
    }

    public boolean markDisconnectedIfCurrentSession(
            Long roomId,
            Long userId,
            String sessionId,
            Instant disconnectedAt,
            Instant expiresAt
    ) {
        Long generation = redisTemplate.execute(
                MARK_DISCONNECTED_SCRIPT,
                List.of(presenceKey(roomId, userId), expirationsKey()),
                sessionId,
                String.valueOf(disconnectedAt.toEpochMilli()),
                String.valueOf(expiresAt.toEpochMilli()),
                roomId + ":" + userId
        );
        return generation != null && generation > 0L;
    }

    public List<RoomPresenceCandidate> findExpiredCandidates(Instant now, int limit) {
        Set<String> members = redisTemplate.opsForZSet()
                .rangeByScore(expirationsKey(), 0, now.toEpochMilli(), 0, Math.max(1, limit));
        if (members == null || members.isEmpty()) {
            return List.of();
        }

        return members.stream()
                .map(RoomPresenceCandidate::parse)
                .toList();
    }

    public boolean isExpiredDisconnected(RoomPresenceCandidate candidate, Instant now) {
        String key = presenceKey(candidate.roomId(), candidate.userId());
        List<Object> fields = redisTemplate.opsForHash()
                .multiGet(key, List.of("status", "generation", "expiresAt"));
        if (fields == null || fields.size() != 3) {
            return false;
        }

        String status = valueOf(fields.get(0));
        Long generation = longValueOf(fields.get(1));
        Long expiresAt = longValueOf(fields.get(2));
        return STATUS_DISCONNECTED.equals(status)
                && Long.valueOf(candidate.generation()).equals(generation)
                && expiresAt != null
                && expiresAt <= now.toEpochMilli();
    }

    public void removeExpirationCandidate(RoomPresenceCandidate candidate) {
        redisTemplate.opsForZSet().remove(expirationsKey(), candidate.member());
    }

    private String valueOf(Object value) {
        if (value == null) {
            return null;
        }
        return value.toString();
    }

    private Long longValueOf(Object value) {
        if (value == null) {
            return null;
        }
        String text = value.toString();
        if (text.isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(text);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private String presenceKey(Long roomId, Long userId) {
        return "room:presence:{" + roomId + "}:" + userId;
    }

    private String expirationsKey() {
        return "room:presence:expirations";
    }
}
