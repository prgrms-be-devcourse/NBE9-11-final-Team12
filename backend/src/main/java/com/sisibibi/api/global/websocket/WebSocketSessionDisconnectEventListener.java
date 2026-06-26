package com.sisibibi.api.global.websocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketSessionDisconnectEventListener {

    private final RedisWebSocketSessionRegistry sessionRegistry;

    @EventListener
    public void handleSessionDisconnect(SessionDisconnectEvent event) {
        try {
            sessionRegistry.unregisterSession(event.getSessionId());
        } catch (RuntimeException e) {
            log.warn(
                    "Failed to unregister WebSocket session from Redis on disconnect event. sessionId={}",
                    event.getSessionId(),
                    e
            );
        }
    }
}
