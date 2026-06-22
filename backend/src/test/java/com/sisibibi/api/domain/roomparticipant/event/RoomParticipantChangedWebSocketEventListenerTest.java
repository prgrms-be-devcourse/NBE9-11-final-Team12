package com.sisibibi.api.domain.roomparticipant.event;

import com.sisibibi.api.domain.roomparticipant.dto.event.RoomParticipantChangedEvent;
import com.sisibibi.api.domain.roomparticipant.dto.event.RoomParticipantEventPayload;
import com.sisibibi.api.domain.roomparticipant.dto.event.RoomParticipantEventType;
import com.sisibibi.api.global.realtime.RealtimeEventPublisher;
import com.sisibibi.api.global.websocket.RoomWebSocketDestinations;
import com.sisibibi.api.global.websocket.WebSocketEventEnvelope;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class RoomParticipantChangedWebSocketEventListenerTest {

    private final RealtimeEventPublisher realtimeEventPublisher =
            mock(RealtimeEventPublisher.class);
    private final RoomParticipantChangedWebSocketEventListener listener =
            new RoomParticipantChangedWebSocketEventListener(realtimeEventPublisher);

    @Test
    void handle_publishesParticipantEnvelopeToParticipantEventTopic() {
        RoomParticipantEventPayload payload = new RoomParticipantEventPayload(
                1L,
                7L,
                3,
                LocalDateTime.of(2026, 6, 18, 12, 0)
        );
        RoomParticipantChangedEvent event = new RoomParticipantChangedEvent(
                RoomParticipantEventType.PARTICIPANT_JOINED,
                1L,
                payload
        );
        ArgumentCaptor<WebSocketEventEnvelope> envelopeCaptor =
                ArgumentCaptor.forClass(WebSocketEventEnvelope.class);

        listener.handle(event);

        verify(realtimeEventPublisher).publish(
                org.mockito.ArgumentMatchers.eq(RoomWebSocketDestinations.participantEvents(1L)),
                envelopeCaptor.capture()
        );
        WebSocketEventEnvelope envelope = envelopeCaptor.getValue();
        assertThat(envelope.eventType()).isEqualTo(
                RoomParticipantEventType.PARTICIPANT_JOINED.name()
        );
        assertThat(envelope.roomId()).isEqualTo(1L);
        assertThat(envelope.data()).isEqualTo(payload);
    }

    @Test
    void handle_doesNotThrow_whenWebSocketPublishFails() {
        RoomParticipantEventPayload payload = new RoomParticipantEventPayload(
                1L,
                7L,
                2,
                LocalDateTime.of(2026, 6, 18, 12, 0)
        );
        RoomParticipantChangedEvent event = new RoomParticipantChangedEvent(
                RoomParticipantEventType.PARTICIPANT_LEFT,
                1L,
                payload
        );
        org.mockito.BDDMockito.willThrow(new RuntimeException("broker down"))
                .given(realtimeEventPublisher)
                .publish(
                        org.mockito.ArgumentMatchers.eq(
                                RoomWebSocketDestinations.participantEvents(1L)
                        ),
                        org.mockito.ArgumentMatchers.any()
                );

        listener.handle(event);
    }
}
