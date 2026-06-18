package com.sisibibi.api.domain.chat.service;

import com.sisibibi.api.domain.chat.dto.response.ChatEventRes;
import com.sisibibi.api.domain.chat.entity.ChatEventType;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class StompChatMessagePublisherTest {

    @Test
    void publish_sendsEventToRoomChatTopic() {
        SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
        StompChatMessagePublisher publisher = new StompChatMessagePublisher(messagingTemplate);
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

        verify(messagingTemplate).convertAndSend("/topic/rooms/1/chat/messages", event);
    }
}
