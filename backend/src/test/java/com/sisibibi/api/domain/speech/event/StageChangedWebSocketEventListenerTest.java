package com.sisibibi.api.domain.speech.event;

import com.sisibibi.api.domain.speech.dto.event.StageChangedEvent;
import com.sisibibi.api.domain.speech.dto.event.StageEventPayload;
import com.sisibibi.api.domain.speech.dto.event.StageEventType;
import com.sisibibi.api.domain.speech.entity.SpeakingQueueStatus;
import com.sisibibi.api.domain.speech.entity.SpeechStance;
import com.sisibibi.api.global.realtime.RealtimeEventPublisher;
import com.sisibibi.api.global.websocket.RoomWebSocketDestinations;
import com.sisibibi.api.global.websocket.WebSocketEventEnvelope;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class StageChangedWebSocketEventListenerTest {

    private final RealtimeEventPublisher realtimeEventPublisher =
            mock(RealtimeEventPublisher.class);
    private final StageChangedWebSocketEventListener listener =
            new StageChangedWebSocketEventListener(realtimeEventPublisher);

    @Test
    void handle_publishesStageEnvelopeToStageEventTopic() {
        StageEventPayload payload = new StageEventPayload(
                1L,
                7L,
                SpeechStance.PRO,
                15,
                SpeakingQueueStatus.ASSIGNED,
                LocalDateTime.of(2026, 6, 18, 12, 0),
                LocalDateTime.of(2026, 6, 18, 12, 2),
                null,
                LocalDateTime.of(2026, 6, 18, 12, 0)
        );
        StageChangedEvent event = new StageChangedEvent(
                StageEventType.SPEAKER_ASSIGNED,
                1L,
                payload
        );
        ArgumentCaptor<WebSocketEventEnvelope> envelopeCaptor =
                ArgumentCaptor.forClass(WebSocketEventEnvelope.class);

        listener.handle(event);

        verify(realtimeEventPublisher).publish(
                org.mockito.ArgumentMatchers.eq(RoomWebSocketDestinations.stageEvents(1L)),
                envelopeCaptor.capture()
        );
        WebSocketEventEnvelope envelope = envelopeCaptor.getValue();
        assertThat(envelope.eventType()).isEqualTo(StageEventType.SPEAKER_ASSIGNED.name());
        assertThat(envelope.roomId()).isEqualTo(1L);
        assertThat(envelope.data()).isEqualTo(payload);
    }

    @Test
    void handle_doesNotThrow_whenWebSocketPublishFails() {
        StageEventPayload payload = new StageEventPayload(
                1L,
                7L,
                SpeechStance.PRO,
                15,
                SpeakingQueueStatus.WAITING,
                null,
                null,
                null,
                LocalDateTime.of(2026, 6, 18, 12, 0)
        );
        StageChangedEvent event = new StageChangedEvent(
                StageEventType.SPEAKING_REQUESTED,
                1L,
                payload
        );
        org.mockito.BDDMockito.willThrow(new RuntimeException("broker down"))
                .given(realtimeEventPublisher)
                .publish(
                        org.mockito.ArgumentMatchers.eq(RoomWebSocketDestinations.stageEvents(1L)),
                        org.mockito.ArgumentMatchers.any()
                );

        listener.handle(event);
    }
}
