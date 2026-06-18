package com.sisibibi.api.domain.chat.service;

import com.sisibibi.api.domain.chat.dto.response.ChatEventRes;
import com.sisibibi.api.domain.chat.entity.ChatEventType;
import com.sisibibi.api.global.websocket.RoomWebSocketDestinations;
import com.sisibibi.api.global.websocket.WebSocketEventPublisher;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class StompChatMessagePublisherTest {

    @Test
    void publish_sendsEventToRoomChatTopic() {
        WebSocketEventPublisher webSocketEventPublisher = mock(WebSocketEventPublisher.class);
        StompChatMessagePublisher publisher = new StompChatMessagePublisher(webSocketEventPublisher);
        ChatEventRes event = new ChatEventRes(
                ChatEventType.MESSAGE_CREATED,
                10L,
                1L,
                2L,
                "tester",
                "hello",
                LocalDateTime.of(2026, 6, 15, 10, 0),
                null
        );

        publisher.publish(event);

        verify(webSocketEventPublisher).publish(
                RoomWebSocketDestinations.chatMessages(1L),
                event
        );
    }
}
