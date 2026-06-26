package com.sisibibi.api.global.websocket;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class RedisWebSocketSessionRegistry {

    private static final String PREFIX = "ws";
    private static final String FIELD_USER_ID = "userId";
    private static final String FIELD_CONNECTED_AT = "connectedAt";
    private static final String FIELD_LAST_SEEN_AT = "lastSeenAt";

    private final StringRedisTemplate redisTemplate;
    private final Duration sessionTtl;

    public RedisWebSocketSessionRegistry(
            StringRedisTemplate redisTemplate,
            @Value("${app.websocket.session-registry.ttl:120s}") Duration sessionTtl
    ) {
        this.redisTemplate = redisTemplate;
        this.sessionTtl = sessionTtl;
    }

    public void registerSession(String sessionId, Long userId) {
        String now = String.valueOf(Instant.now().toEpochMilli());
        String sessionKey = sessionKey(sessionId);
        redisTemplate.opsForHash().putAll(sessionKey, Map.of(
                FIELD_USER_ID, userId.toString(),
                FIELD_CONNECTED_AT, now,
                FIELD_LAST_SEEN_AT, now
        ));
        redisTemplate.opsForSet().add(userSessionsKey(userId), sessionId);
        redisTemplate.expire(sessionKey, sessionTtl);
        redisTemplate.expire(userSessionsKey(userId), sessionTtl);
    }

    public WebSocketRoomSession registerRoomSession(
            String sessionId,
            Long roomId,
            Long userId
    ) {
        registerSessionIfAbsent(sessionId, userId);

        String roomIdValue = roomId.toString();
        redisTemplate.opsForSet().add(sessionRoomsKey(sessionId), roomIdValue);
        redisTemplate.opsForSet().add(roomUserSessionsKey(roomId, userId), sessionId);
        refreshSessionTtl(sessionId, userId, Set.of(roomIdValue));
        return new WebSocketRoomSession(roomId, userId, sessionId);
    }

    public Set<WebSocketRoomSession> unregisterSession(String sessionId) {
        Long userId = findUserId(sessionId);
        Set<String> roomIds = redisTemplate.opsForSet().members(sessionRoomsKey(sessionId));
        if (roomIds == null) {
            roomIds = Set.of();
        }

        Set<WebSocketRoomSession> removedRoomSessions = new LinkedHashSet<>();
        if (userId != null) {
            redisTemplate.opsForSet().remove(userSessionsKey(userId), sessionId);
            for (String roomId : roomIds) {
                Long parsedRoomId = parseLong(roomId);
                if (parsedRoomId == null) {
                    continue;
                }
                redisTemplate.opsForSet().remove(roomUserSessionsKey(parsedRoomId, userId), sessionId);
                removedRoomSessions.add(new WebSocketRoomSession(parsedRoomId, userId, sessionId));
            }
        }

        redisTemplate.delete(sessionKey(sessionId));
        redisTemplate.delete(sessionRoomsKey(sessionId));
        return removedRoomSessions;
    }

    public boolean touchSession(String sessionId) {
        Long userId = findUserId(sessionId);
        if (userId == null) {
            return false;
        }

        Set<String> roomIds = redisTemplate.opsForSet().members(sessionRoomsKey(sessionId));
        if (roomIds == null) {
            roomIds = Set.of();
        }
        redisTemplate.opsForHash()
                .put(sessionKey(sessionId), FIELD_LAST_SEEN_AT, String.valueOf(Instant.now().toEpochMilli()));
        refreshSessionTtl(sessionId, userId, roomIds);
        return true;
    }

    public Set<String> findSessionIdsByUserId(Long userId) {
        return activeSessionIds(userSessionsKey(userId));
    }

    public Set<String> findSessionIdsByRoomAndUserId(Long roomId, Long userId) {
        return activeSessionIds(roomUserSessionsKey(roomId, userId));
    }

    public boolean hasActiveRoomSession(Long roomId, Long userId) {
        return !findSessionIdsByRoomAndUserId(roomId, userId).isEmpty();
    }

    private void registerSessionIfAbsent(String sessionId, Long userId) {
        if (Boolean.TRUE.equals(redisTemplate.hasKey(sessionKey(sessionId)))) {
            touchSession(sessionId);
            return;
        }

        registerSession(sessionId, userId);
    }

    private Set<String> activeSessionIds(String indexKey) {
        Set<String> sessionIds = redisTemplate.opsForSet().members(indexKey);
        if (sessionIds == null || sessionIds.isEmpty()) {
            return Set.of();
        }

        Set<String> activeSessionIds = new LinkedHashSet<>();
        for (String sessionId : sessionIds) {
            if (Boolean.TRUE.equals(redisTemplate.hasKey(sessionKey(sessionId)))) {
                activeSessionIds.add(sessionId);
                continue;
            }

            redisTemplate.opsForSet().remove(indexKey, sessionId);
        }
        return Set.copyOf(activeSessionIds);
    }

    private void refreshSessionTtl(String sessionId, Long userId, Set<String> roomIds) {
        redisTemplate.expire(sessionKey(sessionId), sessionTtl);
        redisTemplate.expire(sessionRoomsKey(sessionId), sessionTtl);
        redisTemplate.expire(userSessionsKey(userId), sessionTtl);

        for (String roomId : roomIds) {
            Long parsedRoomId = parseLong(roomId);
            if (parsedRoomId != null) {
                redisTemplate.expire(roomUserSessionsKey(parsedRoomId, userId), sessionTtl);
            }
        }
    }

    private Long findUserId(String sessionId) {
        Object userId = redisTemplate.opsForHash().get(sessionKey(sessionId), FIELD_USER_ID);
        if (userId == null) {
            return null;
        }
        return parseLong(userId.toString());
    }

    private Long parseLong(String value) {
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException e) {
            log.warn("Invalid WebSocket session registry value. value={}", value);
            return null;
        }
    }

    private String sessionKey(String sessionId) {
        return PREFIX + ":sessions:" + sessionId;
    }

    private String sessionRoomsKey(String sessionId) {
        return PREFIX + ":session-rooms:" + sessionId;
    }

    private String userSessionsKey(Long userId) {
        return PREFIX + ":user-sessions:" + userId;
    }

    private String roomUserSessionsKey(Long roomId, Long userId) {
        return PREFIX + ":room-user-sessions:" + roomId + ":" + userId;
    }
}
