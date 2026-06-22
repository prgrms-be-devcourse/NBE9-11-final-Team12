package com.sisibibi.api.domain.speech.repository;

import com.sisibibi.api.domain.speech.entity.SpeakingQueue;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Repository;

@Repository
public class RedisSpeakingQueueRepository {

    private static final DefaultRedisScript<Long> UPSERT_SCRIPT =
            new DefaultRedisScript<>(
                    """
                    redis.call('ZADD', KEYS[1], ARGV[2], ARGV[1])
                    return redis.call('INCR', KEYS[2])
                    """,
                    Long.class
            );

    private static final DefaultRedisScript<Long> REMOVE_WAITING_SCRIPT =
            new DefaultRedisScript<>(
                    """
                    redis.call('ZREM', KEYS[1], ARGV[1])
                    return redis.call('INCR', KEYS[2])
                    """,
                    Long.class
            );

    private static final DefaultRedisScript<Long> ASSIGN_SCRIPT =
            new DefaultRedisScript<>(
                    """
                    redis.call('ZREM', KEYS[1], ARGV[1])
                    redis.call('SET', KEYS[2], ARGV[1])
                    return redis.call('INCR', KEYS[3])
                    """,
                    Long.class
            );

    private static final DefaultRedisScript<Long> REMOVE_CURRENT_SPEAKER_SCRIPT =
            new DefaultRedisScript<>(
                    """
                    if redis.call('GET', KEYS[1]) == ARGV[1] then
                        redis.call('DEL', KEYS[1])
                        return redis.call('INCR', KEYS[2])
                    end
                    return 0
                    """,
                    Long.class
            );

    private static final DefaultRedisScript<Long> REPLACE_ROOM_PROJECTION_SCRIPT =
            new DefaultRedisScript<>(
                    """
                    local currentVersion = tonumber(redis.call('GET', KEYS[3]) or '0')
                    local expectedVersion = tonumber(ARGV[1])

                    if #ARGV < 2 or ((#ARGV - 2) % 2) ~= 0 then
                        return redis.error_reply('Invalid speaking projection arguments')
                    end

                    if currentVersion == nil then
                        currentVersion = 0
                    end

                    if expectedVersion == nil then
                        return redis.error_reply('Invalid expected projection version')
                    end

                    if currentVersion ~= expectedVersion then
                        return 0
                    end

                    redis.call('DEL', KEYS[1])
                    redis.call('DEL', KEYS[2])

                    local currentSpeakerUserId = ARGV[2]
                    if currentSpeakerUserId ~= '' then
                        redis.call('SET', KEYS[2], currentSpeakerUserId)
                    end

                    local index = 3
                    while index < #ARGV do
                        redis.call('ZADD', KEYS[1], ARGV[index + 1], ARGV[index])
                        index = index + 2
                    end

                    redis.call('SET', KEYS[3], currentVersion + 1)
                    return 1
                    """,
                    Long.class
            );

    private final StringRedisTemplate redisTemplate;

    public RedisSpeakingQueueRepository(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void upsert(Long roomId, Long userId, int queueOrder) {
        redisTemplate.execute(
                UPSERT_SCRIPT,
                List.of(queueKey(roomId), projectionVersionKey(roomId)),
                userId.toString(),
                String.valueOf(queueOrder)
        );
    }

    public void remove(Long roomId, Long userId) {
        redisTemplate.execute(
                REMOVE_WAITING_SCRIPT,
                List.of(queueKey(roomId), projectionVersionKey(roomId)),
                userId.toString()
        );
    }

    public Optional<Integer> rank(Long roomId, Long userId) {
        Long rank = redisTemplate.opsForZSet()
                .rank(queueKey(roomId), userId.toString());

        if (rank == null) {
            return Optional.empty();
        }
        return Optional.of(Math.toIntExact(rank + 1));
    }

    public List<Long> findWaitingUserIds(Long roomId, long start, long end) {
        Set<String> userIds = redisTemplate.opsForZSet()
                .range(queueKey(roomId), start, end);

        if (userIds == null || userIds.isEmpty()) {
            return List.of();
        }

        return userIds.stream()
                .map(Long::valueOf)
                .toList();
    }

    public long count(Long roomId) {
        Long size = redisTemplate.opsForZSet().size(queueKey(roomId));

        if (size == null) {
            return 0L;
        }
        return size;
    }

    public void assign(Long roomId, Long userId) {
        redisTemplate.execute(
                ASSIGN_SCRIPT,
                List.of(
                        queueKey(roomId),
                        currentSpeakerKey(roomId),
                        projectionVersionKey(roomId)
                ),
                userId.toString()
        );
    }

    public void removeCurrentSpeaker(Long roomId, Long userId) {
        redisTemplate.execute(
                REMOVE_CURRENT_SPEAKER_SCRIPT,
                List.of(currentSpeakerKey(roomId), projectionVersionKey(roomId)),
                userId.toString()
        );
    }

    public long currentProjectionVersion(Long roomId) {
        String version = redisTemplate.opsForValue().get(projectionVersionKey(roomId));
        if (version == null) {
            return 0L;
        }
        try {
            return Long.parseLong(version);
        } catch (NumberFormatException ignored) {
            return 0L;
        }
    }

    public boolean replaceRoomProjectionIfVersionMatches(
            Long roomId,
            List<SpeakingQueue> waitingQueues,
            Optional<SpeakingQueue> currentSpeaker,
            long expectedVersion
    ) {
        List<String> arguments = new ArrayList<>();
        arguments.add(String.valueOf(expectedVersion));
        arguments.add(currentSpeaker
                .map(SpeakingQueue::getUserId)
                .map(String::valueOf)
                .orElse(""));

        for (SpeakingQueue waitingQueue : waitingQueues) {
            arguments.add(waitingQueue.getUserId().toString());
            arguments.add(requiredQueueOrder(waitingQueue));
        }

        Long result = redisTemplate.execute(
                REPLACE_ROOM_PROJECTION_SCRIPT,
                List.of(
                        queueKey(roomId),
                        currentSpeakerKey(roomId),
                        projectionVersionKey(roomId)
                ),
                arguments.toArray(Object[]::new)
        );
        return result != null && result == 1L;
    }

    private String requiredQueueOrder(SpeakingQueue waitingQueue) {
        Integer queueOrder = waitingQueue.getQueueOrder();
        if (queueOrder == null) {
            throw new IllegalStateException(
                    "Waiting speaking queue must have queue order to rebuild Redis projection. "
                            + "roomId=" + waitingQueue.getRoomId()
                            + ", userId=" + waitingQueue.getUserId()
            );
        }
        return queueOrder.toString();
    }

    private String queueKey(Long roomId) {
        return "stage:queue:{" + roomId + "}";
    }

    private String currentSpeakerKey(Long roomId) {
        return "stage:current:{" + roomId + "}";
    }

    private String projectionVersionKey(Long roomId) {
        return "stage:projection-version:{" + roomId + "}";
    }
}
