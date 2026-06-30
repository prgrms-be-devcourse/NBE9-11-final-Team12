package com.sisibibi.api.domain.chat.event;

import com.sisibibi.api.domain.chat.dto.event.ChatMessageChangedEvent;
import com.sisibibi.api.domain.chat.dto.event.ChatMessageEventPayload;
import com.sisibibi.api.domain.chat.dto.event.ChatEventType;
import com.sisibibi.api.domain.chat.service.ChatPerformanceMetrics;
import com.sisibibi.api.global.realtime.RealtimeEventPublisher;
import com.sisibibi.api.global.websocket.destination.RoomWebSocketDestinations;
import com.sisibibi.api.global.websocket.WebSocketEventEnvelope;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ChatMessageChangedWebSocketEventListenerTest {

    private final RealtimeEventPublisher realtimeEventPublisher =
            mock(RealtimeEventPublisher.class);
    private final ChatPerformanceMetrics chatPerformanceMetrics =
            mock(ChatPerformanceMetrics.class);
    private final ChatMessageChangedWebSocketEventListener listener =
            new ChatMessageChangedWebSocketEventListener(realtimeEventPublisher, chatPerformanceMetrics);

    @BeforeEach
    void setUp() {
        doAnswer(invocation -> {
            Runnable runnable = invocation.getArgument(0);
            runnable.run();
            return null;
        }).when(chatPerformanceMetrics).recordWebSocketPublish(any());
    }

    @Test
    void handle_publishesChatEnvelopeToChatEventTopic() {
        ChatMessageEventPayload payload = payload(ChatEventType.MESSAGE_CREATED);
        ChatMessageChangedEvent event = new ChatMessageChangedEvent(
                ChatEventType.MESSAGE_CREATED,
                1L,
                payload
        );
        ArgumentCaptor<WebSocketEventEnvelope> envelopeCaptor =
                ArgumentCaptor.forClass(WebSocketEventEnvelope.class);

        listener.handle(event);

        verify(realtimeEventPublisher).publish(
                org.mockito.ArgumentMatchers.eq(RoomWebSocketDestinations.chatEvents(1L)),
                envelopeCaptor.capture()
        );
        WebSocketEventEnvelope envelope = envelopeCaptor.getValue();
        assertThat(envelope.eventType()).isEqualTo(ChatEventType.MESSAGE_CREATED.name());
        assertThat(envelope.roomId()).isEqualTo(1L);
        assertThat(envelope.data()).isEqualTo(payload);
    }

    @Test
    void handle_doesNotThrow_whenWebSocketPublishFails() {
        ChatMessageEventPayload payload = payload(ChatEventType.MESSAGE_DELETED);
        ChatMessageChangedEvent event = new ChatMessageChangedEvent(
                ChatEventType.MESSAGE_DELETED,
                1L,
                payload
        );
        org.mockito.BDDMockito.willThrow(new RuntimeException("broker down"))
                .given(realtimeEventPublisher)
                .publish(
                        org.mockito.ArgumentMatchers.eq(RoomWebSocketDestinations.chatEvents(1L)),
                        org.mockito.ArgumentMatchers.any()
                );

        listener.handle(event);
    }

    private ChatMessageEventPayload payload(ChatEventType type) {
        return new ChatMessageEventPayload(
                type,
                10L,
                1L,
                2L,
                "tester",
                type == ChatEventType.MESSAGE_CREATED ? "hello" : null,
                LocalDateTime.of(2026, 6, 21, 10, 0),
                type == ChatEventType.MESSAGE_DELETED
                        ? LocalDateTime.of(2026, 6, 21, 10, 1)
                        : null
        );
    }
}
