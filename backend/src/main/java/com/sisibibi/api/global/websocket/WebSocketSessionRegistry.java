package com.sisibibi.api.global.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Slf4j
@Component
public class WebSocketSessionRegistry {

    private final ConcurrentMap<String, WebSocketSession> webSocketSessions =
            new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Long> userIdsBySessionId =
            new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, Set<String>> sessionIdsByUserId =
            new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Set<Long>> roomIdsBySessionId =
            new ConcurrentHashMap<>();
    private final ConcurrentMap<RoomUserKey, Set<String>> sessionIdsByRoomUser =
            new ConcurrentHashMap<>();

    public void registerWebSocketSession(WebSocketSession session) {
        webSocketSessions.put(session.getId(), session);
    }

    public void unregisterWebSocketSession(String sessionId) {
        webSocketSessions.remove(sessionId);
    }

    public void bindUser(String sessionId, Long userId) {
        Long previousUserId = userIdsBySessionId.put(sessionId, userId);
        if (previousUserId != null && !previousUserId.equals(userId)) {
            removeSessionId(sessionIdsByUserId, previousUserId, sessionId);
        }

        sessionIdsByUserId
                .computeIfAbsent(userId, ignored -> ConcurrentHashMap.newKeySet())
                .add(sessionId);
    }

    public void bindRoom(String sessionId, Long userId, Long roomId) {
        bindUser(sessionId, userId);
        roomIdsBySessionId
                .computeIfAbsent(sessionId, ignored -> ConcurrentHashMap.newKeySet())
                .add(roomId);
        sessionIdsByRoomUser
                .computeIfAbsent(new RoomUserKey(roomId, userId), ignored -> ConcurrentHashMap.newKeySet())
                .add(sessionId);
    }

    public WebSocketSessionState unregisterSessionState(String sessionId) {
        Long userId = userIdsBySessionId.remove(sessionId);
        if (userId != null) {
            removeSessionId(sessionIdsByUserId, userId, sessionId);
        }

        Set<Long> roomIds = roomIdsBySessionId.remove(sessionId);
        if (userId != null && roomIds != null) {
            roomIds.forEach(roomId ->
                    removeSessionId(sessionIdsByRoomUser, new RoomUserKey(roomId, userId), sessionId)
            );
        }

        return new WebSocketSessionState(sessionId, userId, copyOf(roomIds));
    }

    public Set<String> findSessionIdsByUserId(Long userId) {
        return copyOf(sessionIdsByUserId.get(userId));
    }

    public Set<Long> findRoomIdsBySessionId(String sessionId) {
        return copyOf(roomIdsBySessionId.get(sessionId));
    }

    public Set<String> findSessionIdsByRoomAndUser(Long roomId, Long userId) {
        return copyOf(sessionIdsByRoomUser.get(new RoomUserKey(roomId, userId)));
    }

    public int closeUserSessions(Long userId, CloseStatus closeStatus) {
        int closedCount = 0;
        for (String sessionId : findSessionIdsByUserId(userId)) {
            WebSocketSession session = webSocketSessions.get(sessionId);
            if (session == null || !session.isOpen()) {
                continue;
            }

            try {
                session.close(closeStatus);
                closedCount++;
            } catch (IOException closeException) {
                log.warn("Failed to close WebSocket session. userId={}, sessionId={}",
                        userId,
                        sessionId,
                        closeException);
            }
        }
        return closedCount;
    }

    private <K> void removeSessionId(
            ConcurrentMap<K, Set<String>> sessionsByKey,
            K key,
            String sessionId
    ) {
        sessionsByKey.computeIfPresent(key, (ignored, sessionIds) -> {
            sessionIds.remove(sessionId);
            if (sessionIds.isEmpty()) {
                return null;
            }
            return sessionIds;
        });
    }

    private <T> Set<T> copyOf(Set<T> values) {
        if (values == null || values.isEmpty()) {
            return Collections.emptySet();
        }
        return Set.copyOf(values);
    }

    private record RoomUserKey(Long roomId, Long userId) {
    }
}
