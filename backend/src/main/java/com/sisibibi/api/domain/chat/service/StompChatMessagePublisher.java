package com.sisibibi.api.domain.chat.service;

import com.sisibibi.api.domain.chat.dto.response.ChatEventRes;
import com.sisibibi.api.global.websocket.RoomWebSocketDestinations;
import com.sisibibi.api.global.websocket.WebSocketEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StompChatMessagePublisher implements ChatMessagePublisher {

    private final WebSocketEventPublisher webSocketEventPublisher;

    @Override
    public void publish(ChatEventRes event) {
        webSocketEventPublisher.publish(
                RoomWebSocketDestinations.chatMessages(event.roomId()),
                event
        );
    }
}
