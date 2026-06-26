package com.sisibibi.api.global.websocket;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

class WebSocketSessionDisconnectEventListenerTest {

    private final RedisWebSocketSessionRegistry sessionRegistry =
            mock(RedisWebSocketSessionRegistry.class);
    private final WebSocketSessionDisconnectEventListener listener =
            new WebSocketSessionDisconnectEventListener(sessionRegistry);

    @Test
    void handleSessionDisconnect_unregistersSession() {
        Message<byte[]> message = MessageBuilder.withPayload(new byte[0]).build();
        SessionDisconnectEvent event = new SessionDisconnectEvent(
                this,
                message,
                "session-1",
                CloseStatus.NORMAL
        );

        listener.handleSessionDisconnect(event);

        verify(sessionRegistry).unregisterSession("session-1");
    }
}
