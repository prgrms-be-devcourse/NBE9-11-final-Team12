package com.sisibibi.api.global.websocket;

import com.sisibibi.api.global.security.AuthPrincipal;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

import java.security.Principal;

import static org.assertj.core.api.Assertions.assertThat;

class WebSocketSessionRegistryEventListenerTest {

    private final WebSocketSessionRegistry registry = new WebSocketSessionRegistry();
    private final WebSocketSessionRegistryEventListener listener =
            new WebSocketSessionRegistryEventListener(registry);

    @Test
    void handleSessionConnect_tracksUserSession() {
        Principal user = authenticatedPrincipal(1L);

        listener.handleSessionConnect(new SessionConnectEvent(
                this,
                message(StompCommand.CONNECT, "session-1", null, user),
                user
        ));

        assertThat(registry.findSessionIdsByUserId(1L)).containsExactly("session-1");
    }

    @Test
    void handleSessionSubscribe_tracksAllowedRoomTopic() {
        Principal user = authenticatedPrincipal(1L);

        listener.handleSessionSubscribe(new SessionSubscribeEvent(
                this,
                message(StompCommand.SUBSCRIBE, "session-1", "/topic/rooms/10/chat/events", user),
                user
        ));

        assertThat(registry.findRoomIdsBySessionId("session-1")).containsExactly(10L);
        assertThat(registry.findSessionIdsByRoomAndUser(10L, 1L)).containsExactly("session-1");
    }

    @Test
    void handleSessionSubscribe_ignoresNonRoomTopic() {
        Principal user = authenticatedPrincipal(1L);

        listener.handleSessionSubscribe(new SessionSubscribeEvent(
                this,
                message(StompCommand.SUBSCRIBE, "session-1", "/topic/users/1/sanctions/events", user),
                user
        ));

        assertThat(registry.findRoomIdsBySessionId("session-1")).isEmpty();
    }

    @Test
    void handleSessionDisconnect_unregistersSessionState() {
        Principal user = authenticatedPrincipal(1L);
        listener.handleSessionSubscribe(new SessionSubscribeEvent(
                this,
                message(StompCommand.SUBSCRIBE, "session-1", "/topic/rooms/10/chat/events", user),
                user
        ));

        listener.handleSessionDisconnect(new SessionDisconnectEvent(
                this,
                message(StompCommand.DISCONNECT, "session-1", null, user),
                "session-1",
                CloseStatus.NORMAL,
                user
        ));

        assertThat(registry.findSessionIdsByUserId(1L)).isEmpty();
        assertThat(registry.findRoomIdsBySessionId("session-1")).isEmpty();
        assertThat(registry.findSessionIdsByRoomAndUser(10L, 1L)).isEmpty();
    }

    private Message<byte[]> message(
            StompCommand command,
            String sessionId,
            String destination,
            Principal user
    ) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
        accessor.setSessionId(sessionId);
        accessor.setDestination(destination);
        accessor.setUser(user);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    private Principal authenticatedPrincipal(Long userId) {
        AuthPrincipal principal = new AuthPrincipal(userId, "user" + userId + "@example.com", "USER");
        return new UsernamePasswordAuthenticationToken(principal, null);
    }
}
