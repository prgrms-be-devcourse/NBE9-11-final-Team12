package com.sisibibi.api.global.websocket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.time.Duration;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

class RedisWebSocketSessionRegistryTest {

    private StringRedisTemplate redisTemplate;
    private HashOperations<String, Object, Object> hashOperations;
    private SetOperations<String, String> setOperations;
    private RedisWebSocketSessionRegistry registry;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        redisTemplate = mock(StringRedisTemplate.class);
        hashOperations = mock(HashOperations.class);
        setOperations = mock(SetOperations.class);
        given(redisTemplate.opsForHash()).willReturn(hashOperations);
        given(redisTemplate.opsForSet()).willReturn(setOperations);
        registry = new RedisWebSocketSessionRegistry(redisTemplate, Duration.ofSeconds(120));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Test
    void registerSession_storesSessionHashAndUserIndexWithTtl() {
        registry.registerSession("session-1", 2L);

        ArgumentCaptor<Map<String, String>> hashCaptor = ArgumentCaptor.forClass(Map.class);
        verify(hashOperations).putAll(eq("ws:sessions:session-1"), hashCaptor.capture());
        assertThat(hashCaptor.getValue()).containsEntry("userId", "2");
        assertThat(hashCaptor.getValue()).containsKeys("connectedAt", "lastSeenAt");
        verify(setOperations).add("ws:user-sessions:2", "session-1");
        verify(redisTemplate).expire("ws:sessions:session-1", Duration.ofSeconds(120));
        verify(redisTemplate).expire("ws:user-sessions:2", Duration.ofSeconds(120));
    }

    @Test
    void registerRoomSession_storesRoomIndexes() {
        given(redisTemplate.hasKey("ws:sessions:session-1")).willReturn(false);

        WebSocketRoomSession roomSession =
                registry.registerRoomSession("session-1", 1L, 2L);

        assertThat(roomSession).isEqualTo(new WebSocketRoomSession(1L, 2L, "session-1"));
        verify(setOperations).add("ws:session-rooms:session-1", "1");
        verify(setOperations).add("ws:room-user-sessions:1:2", "session-1");
        verify(redisTemplate).expire("ws:session-rooms:session-1", Duration.ofSeconds(120));
        verify(redisTemplate).expire("ws:room-user-sessions:1:2", Duration.ofSeconds(120));
    }

    @Test
    void unregisterSession_removesSessionFromKnownIndexes() {
        given(hashOperations.get("ws:sessions:session-1", "userId")).willReturn("2");
        given(setOperations.members("ws:session-rooms:session-1")).willReturn(Set.of("1", "3"));

        Set<WebSocketRoomSession> removedSessions =
                registry.unregisterSession("session-1");

        assertThat(removedSessions).containsExactlyInAnyOrder(
                new WebSocketRoomSession(1L, 2L, "session-1"),
                new WebSocketRoomSession(3L, 2L, "session-1")
        );
        verify(setOperations).remove("ws:user-sessions:2", "session-1");
        verify(setOperations).remove("ws:room-user-sessions:1:2", "session-1");
        verify(setOperations).remove("ws:room-user-sessions:3:2", "session-1");
        verify(redisTemplate).delete("ws:sessions:session-1");
        verify(redisTemplate).delete("ws:session-rooms:session-1");
    }

    @Test
    void findSessionIdsByUserId_returnsOnlyActiveSessionsAndRemovesStaleReferences() {
        given(setOperations.members("ws:user-sessions:2"))
                .willReturn(Set.of("session-1", "session-2"));
        given(redisTemplate.hasKey("ws:sessions:session-1")).willReturn(true);
        given(redisTemplate.hasKey("ws:sessions:session-2")).willReturn(false);

        Set<String> sessionIds = registry.findSessionIdsByUserId(2L);

        assertThat(sessionIds).containsExactlyInAnyOrder("session-1");
        verify(setOperations).remove("ws:user-sessions:2", "session-2");
    }

    @Test
    void touchSession_refreshesSessionAndRoomIndexes() {
        given(hashOperations.get("ws:sessions:session-1", "userId")).willReturn("2");
        given(setOperations.members("ws:session-rooms:session-1")).willReturn(Set.of("1"));

        boolean touched = registry.touchSession("session-1");

        assertThat(touched).isTrue();
        verify(hashOperations).put(eq("ws:sessions:session-1"), eq("lastSeenAt"), any(String.class));
        verify(redisTemplate).expire("ws:sessions:session-1", Duration.ofSeconds(120));
        verify(redisTemplate).expire("ws:session-rooms:session-1", Duration.ofSeconds(120));
        verify(redisTemplate).expire("ws:user-sessions:2", Duration.ofSeconds(120));
        verify(redisTemplate).expire("ws:room-user-sessions:1:2", Duration.ofSeconds(120));
    }
}
