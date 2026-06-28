package com.sisibibi.api.global.websocket;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.WebSocketHandlerDecorator;
import org.springframework.web.socket.handler.WebSocketHandlerDecoratorFactory;

@Component
@RequiredArgsConstructor
public class RegistryWebSocketHandlerDecoratorFactory implements WebSocketHandlerDecoratorFactory {

    private final WebSocketSessionRegistry sessionRegistry;

    @Override
    public WebSocketHandler decorate(WebSocketHandler handler) {
        return new WebSocketHandlerDecorator(handler) {
            @Override
            public void afterConnectionEstablished(WebSocketSession session) throws Exception {
                super.afterConnectionEstablished(session);
                sessionRegistry.registerWebSocketSession(session);
            }

            @Override
            public void afterConnectionClosed(
                    WebSocketSession session,
                    CloseStatus closeStatus
            ) throws Exception {
                try {
                    super.afterConnectionClosed(session, closeStatus);
                } finally {
                    sessionRegistry.unregisterWebSocketSession(session.getId());
                }
            }
        };
    }
}
