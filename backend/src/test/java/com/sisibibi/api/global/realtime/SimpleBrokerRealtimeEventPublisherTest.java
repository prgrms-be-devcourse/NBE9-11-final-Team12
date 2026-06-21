package com.sisibibi.api.global.realtime;

import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class SimpleBrokerRealtimeEventPublisherTest {

    @Test
    void publish_delegatesToSimpMessagingTemplate() {
        SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
        SimpleBrokerRealtimeEventPublisher publisher =
                new SimpleBrokerRealtimeEventPublisher(messagingTemplate);
        Object event = new Object();

        publisher.publish("/topic/rooms/1/chat/events", event);

        verify(messagingTemplate).convertAndSend("/topic/rooms/1/chat/events", event);
    }
}
