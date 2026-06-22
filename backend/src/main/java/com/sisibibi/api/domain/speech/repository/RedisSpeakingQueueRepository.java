package com.sisibibi.api.domain.speech.repository;

import com.sisibibi.api.domain.speech.entity.SpeakingQueue;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
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

    private static final DefaultRedisScript<Long> REMOVE_CURRENT_SPEAKER_SCRIPT =
            new DefaultRedisScript<>(
                    """
                    if redis.call('GET', KEYS[1]) == ARGV[1] then
                        return redis.call('DEL', KEYS[1])
                    end
                    return 0
                    """,
                    Long.class
            );

    private static final DefaultRedisScript<Long> REPLACE_ROOM_PROJECTION_SCRIPT =
            new DefaultRedisScript<>(
                    """
                    redis.call('DEL', KEYS[1])
                    redis.call('DEL', KEYS[2])

                    local currentSpeakerUserId = ARGV[1]
                    if currentSpeakerUserId ~= '' then
                        redis.call('SET', KEYS[2], currentSpeakerUserId)
                    end

                    local index = 2
                    while index <= #ARGV do
                        redis.call('ZADD', KEYS[1], ARGV[index + 1], ARGV[index])
                        index = index + 2
                    end

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
                List.of(queueKey(roomId), currentSpeakerKey(roomId)),
                userId.toString()
        );
    }

    public void removeCurrentSpeaker(Long roomId, Long userId) {
        redisTemplate.execute(
                REMOVE_CURRENT_SPEAKER_SCRIPT,
                List.of(currentSpeakerKey(roomId)),
                userId.toString()
        );
    }

    public void replaceRoomProjection(
            Long roomId,
            List<SpeakingQueue> waitingQueues,
            Optional<SpeakingQueue> currentSpeaker
    ) {
        List<String> arguments = new ArrayList<>();
        arguments.add(currentSpeaker
                .map(SpeakingQueue::getUserId)
                .map(String::valueOf)
                .orElse(""));

        for (SpeakingQueue waitingQueue : waitingQueues) {
            arguments.add(waitingQueue.getUserId().toString());
            arguments.add(Objects.requireNonNull(
                    waitingQueue.getQueueOrder(),
                    "Queue order is required to rebuild Redis speaking queue."
            ).toString());
        }

        redisTemplate.execute(
                REPLACE_ROOM_PROJECTION_SCRIPT,
                List.of(queueKey(roomId), currentSpeakerKey(roomId)),
                arguments.toArray(Object[]::new)
        );
    }

    private String queueKey(Long roomId) {
        return "stage:queue:{" + roomId + "}";
    }

    private String currentSpeakerKey(Long roomId) {
        return "stage:current:{" + roomId + "}";
    }
}
