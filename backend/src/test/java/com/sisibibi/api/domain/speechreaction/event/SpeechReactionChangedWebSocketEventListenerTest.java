package com.sisibibi.api.domain.speechreaction.event;

import com.sisibibi.api.domain.speechreaction.dto.event.SpeechReactionChangedEvent;
import com.sisibibi.api.domain.speechreaction.dto.event.SpeechReactionEventPayload;
import com.sisibibi.api.domain.speechreaction.dto.event.SpeechReactionEventType;
import com.sisibibi.api.global.websocket.RoomWebSocketDestinations;
import com.sisibibi.api.global.websocket.WebSocketEventEnvelope;
import com.sisibibi.api.global.websocket.WebSocketEventPublisher;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class SpeechReactionChangedWebSocketEventListenerTest {

    private final WebSocketEventPublisher webSocketEventPublisher =
            mock(WebSocketEventPublisher.class);
    private final SpeechReactionChangedWebSocketEventListener listener =
            new SpeechReactionChangedWebSocketEventListener(webSocketEventPublisher);

    @Test
    void handle_publishesReactionEnvelopeToSpeechReactionEventTopic() {
        SpeechReactionEventPayload payload = new SpeechReactionEventPayload(
                1L,
                10L,
                3L,
                LocalDateTime.of(2026, 6, 21, 12, 0)
        );
        SpeechReactionChangedEvent event = new SpeechReactionChangedEvent(
                SpeechReactionEventType.SPEECH_REACTION_CHANGED,
                1L,
                payload
        );
        ArgumentCaptor<WebSocketEventEnvelope> envelopeCaptor =
                ArgumentCaptor.forClass(WebSocketEventEnvelope.class);

        listener.handle(event);

        verify(webSocketEventPublisher).publish(
                org.mockito.ArgumentMatchers.eq(
                        RoomWebSocketDestinations.speechReactionEvents(1L)
                ),
                envelopeCaptor.capture()
        );
        WebSocketEventEnvelope envelope = envelopeCaptor.getValue();
        assertThat(envelope.eventType()).isEqualTo(
                SpeechReactionEventType.SPEECH_REACTION_CHANGED.name()
        );
        assertThat(envelope.roomId()).isEqualTo(1L);
        assertThat(envelope.data()).isEqualTo(payload);
    }

    @Test
    void handle_doesNotThrow_whenWebSocketPublishFails() {
        SpeechReactionEventPayload payload = new SpeechReactionEventPayload(
                1L,
                10L,
                2L,
                LocalDateTime.of(2026, 6, 21, 12, 0)
        );
        SpeechReactionChangedEvent event = new SpeechReactionChangedEvent(
                SpeechReactionEventType.SPEECH_REACTION_CHANGED,
                1L,
                payload
        );
        org.mockito.BDDMockito.willThrow(new RuntimeException("broker down"))
                .given(webSocketEventPublisher)
                .publish(
                        org.mockito.ArgumentMatchers.eq(
                                RoomWebSocketDestinations.speechReactionEvents(1L)
                        ),
                        org.mockito.ArgumentMatchers.any()
                );

        listener.handle(event);
    }
}
