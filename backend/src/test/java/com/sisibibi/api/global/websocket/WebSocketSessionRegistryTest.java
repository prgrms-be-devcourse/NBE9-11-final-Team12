package com.sisibibi.api.global.websocket;

import org.junit.jupiter.api.Test;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WebSocketSessionRegistryTest {

    private final WebSocketSessionRegistry registry = new WebSocketSessionRegistry();

    @Test
    void bindRoom_tracksSessionByUserAndRoom() {
        registry.bindRoom("session-1", 1L, 10L);
        registry.bindRoom("session-2", 1L, 10L);
        registry.bindRoom("session-3", 2L, 10L);

        assertThat(registry.findSessionIdsByUserId(1L))
                .containsExactlyInAnyOrder("session-1", "session-2");
        assertThat(registry.findRoomIdsBySessionId("session-1"))
                .containsExactly(10L);
        assertThat(registry.findSessionIdsByRoomAndUser(10L, 1L))
                .containsExactlyInAnyOrder("session-1", "session-2");
    }

    @Test
    void unregisterSessionState_removesAllSessionIndexes() {
        registry.bindRoom("session-1", 1L, 10L);
        registry.bindRoom("session-1", 1L, 20L);

        WebSocketSessionState state = registry.unregisterSessionState("session-1");

        assertThat(state.sessionId()).isEqualTo("session-1");
        assertThat(state.userId()).isEqualTo(1L);
        assertThat(state.roomIds()).containsExactlyInAnyOrder(10L, 20L);
        assertThat(registry.findSessionIdsByUserId(1L)).isEmpty();
        assertThat(registry.findRoomIdsBySessionId("session-1")).isEmpty();
        assertThat(registry.findSessionIdsByRoomAndUser(10L, 1L)).isEmpty();
        assertThat(registry.findSessionIdsByRoomAndUser(20L, 1L)).isEmpty();
    }

    @Test
    void closeUserSessions_closesOpenWebSocketSessions() throws Exception {
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getId()).thenReturn("session-1");
        when(session.isOpen()).thenReturn(true);
        registry.registerWebSocketSession(session);
        registry.bindUser("session-1", 1L);

        int closedCount = registry.closeUserSessions(1L, CloseStatus.POLICY_VIOLATION);

        assertThat(closedCount).isEqualTo(1);
        verify(session).close(CloseStatus.POLICY_VIOLATION);
    }
}
