package com.sisibibi.api.domain.roomranking.repository;

import java.util.List;
import java.util.Set;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Repository;

@Repository
public class RedisRoomRankingRepository {

  private static final String PARTICIPANT_COUNT_KEY = "room:ranking:participant-count";
  private static final String CHAT_MESSAGE_COUNT_KEY = "room:ranking:chat-message-count";
  private static final String REACTION_COUNT_KEY = "room:ranking:reaction-count";
  private static final String RANKING_KEY = "room:ranking";

  private static final int CHAT_MESSAGE_STAT_KEY_INDEX = 2;

  private static final DefaultRedisScript<Long> INCREASE_STAT_AND_REFRESH_SCORE_SCRIPT =
      new DefaultRedisScript<>(
          """
          local roomId = ARGV[1]
          local statKeyIndex = tonumber(ARGV[2])
          local delta = tonumber(ARGV[3])

          local nextCount = redis.call('HINCRBY', KEYS[statKeyIndex], roomId, delta)
          if nextCount < 0 then
            redis.call('HSET', KEYS[statKeyIndex], roomId, 0)
          end

          local participantCount = tonumber(redis.call('HGET', KEYS[1], roomId) or '0')
          local chatMessageCount = tonumber(redis.call('HGET', KEYS[2], roomId) or '0')
          local reactionCount = tonumber(redis.call('HGET', KEYS[3], roomId) or '0')

          local score =
            math.log(1 + participantCount) * 0.5
            + math.log(1 + chatMessageCount) * 0.3
            + math.log(1 + reactionCount) * 0.2

          redis.call('ZADD', KEYS[4], score, roomId)
          return 1
          """,
          Long.class
      );

  private static final DefaultRedisScript<Long> SET_STATS_AND_REFRESH_SCORE_SCRIPT =
      new DefaultRedisScript<>(
          """
          local roomId = ARGV[1]
          local participantCount = tonumber(ARGV[2])
          local chatMessageCount = tonumber(ARGV[3])
          local reactionCount = tonumber(ARGV[4])

          redis.call('HSET', KEYS[1], roomId, participantCount)
          redis.call('HSET', KEYS[2], roomId, chatMessageCount)
          redis.call('HSET', KEYS[3], roomId, reactionCount)

          local score =
            math.log(1 + participantCount) * 0.5
            + math.log(1 + chatMessageCount) * 0.3
            + math.log(1 + reactionCount) * 0.2

          redis.call('ZADD', KEYS[4], score, roomId)
          return 1
          """,
          Long.class
      );

  private static final DefaultRedisScript<Long> REMOVE_ROOM_SCRIPT =
      new DefaultRedisScript<>(
          """
          local roomId = ARGV[1]

          redis.call('HDEL', KEYS[1], roomId)
          redis.call('HDEL', KEYS[2], roomId)
          redis.call('HDEL', KEYS[3], roomId)
          redis.call('ZREM', KEYS[4], roomId)

          return 1
          """,
          Long.class
      );

  private final StringRedisTemplate redisTemplate;

  public RedisRoomRankingRepository(StringRedisTemplate redisTemplate) {
    this.redisTemplate = redisTemplate;
  }

  public void setStatsAndRefreshScore(
      Long roomId,
      long participantCount,
      long chatMessageCount,
      long reactionCount
  ) {
    redisTemplate.execute(
        SET_STATS_AND_REFRESH_SCORE_SCRIPT,
        rankingKeys(),
        roomId.toString(),
        String.valueOf(participantCount),
        String.valueOf(chatMessageCount),
        String.valueOf(reactionCount)
    );
  }

  public void setParticipantCount(Long roomId, long participantCount) {
    setStatsAndRefreshScore(
        roomId,
        participantCount,
        getCount(CHAT_MESSAGE_COUNT_KEY, roomId),
        getCount(REACTION_COUNT_KEY, roomId)
    );
  }

  public void setReactionCount(Long roomId, long reactionCount) {
    setStatsAndRefreshScore(
        roomId,
        getCount(PARTICIPANT_COUNT_KEY, roomId),
        getCount(CHAT_MESSAGE_COUNT_KEY, roomId),
        reactionCount
    );
  }

  public void increaseChatMessageCount(Long roomId) {
    increaseStatAndRefreshScore(roomId, 1);
  }

  public void decreaseChatMessageCount(Long roomId) {
    increaseStatAndRefreshScore(roomId, -1);
  }

  public List<RoomRankingEntry> findTopRooms(long limit) {
    Set<ZSetOperations.TypedTuple<String>> tuples = redisTemplate.opsForZSet()
        .reverseRangeWithScores(RANKING_KEY, 0, limit - 1);

    if (tuples == null || tuples.isEmpty()) {
      return List.of();
    }

    return tuples.stream()
        .map(tuple -> new RoomRankingEntry(Long.valueOf(tuple.getValue()), tuple.getScore()))
        .toList();
  }

  public void removeRoom(Long roomId) {
    redisTemplate.execute(REMOVE_ROOM_SCRIPT, rankingKeys(), roomId.toString());
  }

  private void increaseStatAndRefreshScore(Long roomId, int delta) {
    redisTemplate.execute(
        INCREASE_STAT_AND_REFRESH_SCORE_SCRIPT,
        rankingKeys(),
        roomId.toString(),
        String.valueOf(CHAT_MESSAGE_STAT_KEY_INDEX),
        String.valueOf(delta)
    );
  }

  private long getCount(String key, Long roomId) {
    String value = (String) redisTemplate.opsForHash().get(key, roomId.toString());
    if (value == null) {
      return 0L;
    }
    return Long.parseLong(value);
  }

  private List<String> rankingKeys() {
    return List.of(
        PARTICIPANT_COUNT_KEY,
        CHAT_MESSAGE_COUNT_KEY,
        REACTION_COUNT_KEY,
        RANKING_KEY
    );
  }

  public record RoomRankingEntry(Long roomId, Double score) {
  }
}