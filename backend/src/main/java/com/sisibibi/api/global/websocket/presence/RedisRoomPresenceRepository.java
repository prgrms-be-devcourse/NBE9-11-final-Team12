package com.sisibibi.api.global.websocket.presence;

import java.time.Instant;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Repository;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Repository
public class RedisRoomPresenceRepository {

    private static final String STATUS_CONNECTED = "CONNECTED";
    private static final String STATUS_DISCONNECTED = "DISCONNECTED";
    private static final int MAX_SCAN_COUNT = 1000;
    private static final int ROOM_DELETE_BATCH_SIZE = 1000;

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

                    local generation = tonumber(redis.call('HGET', KEYS[1], 'generation') or '0')
                    if generation <= 0 then
                        generation = 1
                        redis.call('HSET', KEYS[1], 'generation', generation)
                    end

                    redis.call(
                        'HSET',
                        KEYS[1],
                        'status', 'DISCONNECTED',
                        'disconnectedAt', ARGV[2],
                        'expiresAt', ARGV[3]
                    )
                    redis.call('ZADD', KEYS[2], ARGV[3], ARGV[4] .. ':' .. tostring(generation))
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

    public long incrementExpirationFailure(RoomPresenceCandidate candidate) {
        Long count = redisTemplate.opsForHash()
                .increment(expirationFailuresKey(), candidate.member(), 1L);
        return count == null ? 0L : count;
    }

    public void removeExpirationFailure(RoomPresenceCandidate candidate) {
        redisTemplate.opsForHash().delete(expirationFailuresKey(), candidate.member());
    }

    public void deletePresence(Long roomId, Long userId) {
        redisTemplate.delete(presenceKey(roomId, userId));
    }

    public long deleteRoomPresence(Long roomId) {
        long deletedCount = 0L;
        List<String> keys;
        do {
            keys = scanPresenceKeys(roomPresencePattern(roomId), ROOM_DELETE_BATCH_SIZE);
            deletedCount += deleteKeys(keys);
        } while (keys.size() == ROOM_DELETE_BATCH_SIZE);

        return deletedCount;
    }

    private long deleteKeys(List<String> keys) {
        if (keys == null || keys.isEmpty()) {
            return 0L;
        }

        Long deletedCount = redisTemplate.delete(keys);
        return deletedCount == null ? 0L : deletedCount;
    }

    public long cleanupExpiredDisconnectedPresence(Instant cutoff, int limit) {
        List<String> keys = scanPresenceKeys(allRoomPresencePattern(), Math.max(1, limit));
        List<String> keysToDelete = new ArrayList<>();
        for (String key : keys) {
            List<Object> fields = redisTemplate.opsForHash()
                    .multiGet(key, List.of("status", "expiresAt"));
            if (fields == null || fields.size() != 2) {
                continue;
            }

            String status = valueOf(fields.get(0));
            Long expiresAt = longValueOf(fields.get(1));
            if (STATUS_DISCONNECTED.equals(status)
                    && expiresAt != null
                    && expiresAt <= cutoff.toEpochMilli()) {
                keysToDelete.add(key);
            }
        }

        return deleteKeys(keysToDelete);
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

    private String roomPresencePattern(Long roomId) {
        return "room:presence:{" + roomId + "}:*";
    }

    private String allRoomPresencePattern() {
        return "room:presence:{*}:*";
    }

    private String expirationsKey() {
        return "room:presence:expirations";
    }

    private String expirationFailuresKey() {
        return "room:presence:expiration-failures";
    }

    private List<String> scanPresenceKeys(String pattern, int limit) {
        List<String> scannedKeys = redisTemplate.execute((RedisConnection connection) -> {
            List<String> keys = new ArrayList<>();
            ScanOptions options = ScanOptions.scanOptions()
                    .match(pattern)
                    .count(Math.min(Math.max(1, limit), MAX_SCAN_COUNT))
                    .build();
            Cursor<byte[]> cursor = connection.keyCommands().scan(options);
            if (cursor == null) {
                return keys;
            }

            try (cursor) {
                while (cursor.hasNext() && keys.size() < limit) {
                    keys.add(new String(cursor.next(), StandardCharsets.UTF_8));
                }
            } catch (RuntimeException scanException) {
                log.warn("Failed to scan room presence keys. pattern={}, limit={}",
                        pattern,
                        limit,
                        scanException);
            }
            return keys;
        });
        return scannedKeys == null ? List.of() : scannedKeys;
    }
}
