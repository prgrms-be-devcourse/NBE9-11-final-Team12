package com.sisibibi.api.domain.speech.event;

import com.sisibibi.api.domain.speech.dto.event.SpeechChangedEvent;
import com.sisibibi.api.domain.speech.dto.event.SpeechEventPayload;
import com.sisibibi.api.domain.speech.dto.event.SpeechEventType;
import com.sisibibi.api.global.realtime.RealtimeEventPublisher;
import com.sisibibi.api.global.websocket.destination.RoomWebSocketDestinations;
import com.sisibibi.api.global.websocket.WebSocketEventEnvelope;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class SpeechChangedWebSocketEventListenerTest {

    private final RealtimeEventPublisher realtimeEventPublisher =
            mock(RealtimeEventPublisher.class);
    private final SpeechChangedWebSocketEventListener listener =
            new SpeechChangedWebSocketEventListener(realtimeEventPublisher);

    @Test
    void handle_publishesSpeechEnvelopeToSpeechEventTopic() {
        SpeechEventPayload payload = new SpeechEventPayload(
                1L,
                10L,
                2L,
                LocalDateTime.of(2026, 6, 24, 12, 0)
        );
        SpeechChangedEvent event = new SpeechChangedEvent(
                SpeechEventType.SPEECH_UPDATED,
                1L,
                payload
        );
        ArgumentCaptor<WebSocketEventEnvelope> envelopeCaptor =
                ArgumentCaptor.forClass(WebSocketEventEnvelope.class);

        listener.handle(event);

        verify(realtimeEventPublisher).publish(
                org.mockito.ArgumentMatchers.eq(RoomWebSocketDestinations.speechEvents(1L)),
                envelopeCaptor.capture()
        );
        WebSocketEventEnvelope envelope = envelopeCaptor.getValue();
        assertThat(envelope.eventType()).isEqualTo(SpeechEventType.SPEECH_UPDATED.name());
        assertThat(envelope.roomId()).isEqualTo(1L);
        assertThat(envelope.data()).isEqualTo(payload);
    }

    @Test
    void handle_doesNotThrow_whenWebSocketPublishFails() {
        SpeechEventPayload payload = new SpeechEventPayload(
                1L,
                10L,
                2L,
                LocalDateTime.of(2026, 6, 24, 12, 0)
        );
        SpeechChangedEvent event = new SpeechChangedEvent(
                SpeechEventType.SPEECH_DELETED,
                1L,
                payload
        );
        org.mockito.BDDMockito.willThrow(new RuntimeException("broker down"))
                .given(realtimeEventPublisher)
                .publish(
                        org.mockito.ArgumentMatchers.eq(
                                RoomWebSocketDestinations.speechEvents(1L)
                        ),
                        org.mockito.ArgumentMatchers.any()
                );

        listener.handle(event);
    }
}
