package com.sisibibi.api.domain.room.event;

import com.sisibibi.api.domain.room.dto.event.RoomClosedEvent;
import com.sisibibi.api.domain.room.dto.event.RoomClosedEventPayload;
import com.sisibibi.api.domain.room.dto.event.RoomEventType;
import com.sisibibi.api.domain.room.entity.RoomStatus;
import com.sisibibi.api.global.realtime.RealtimeEventPublisher;
import com.sisibibi.api.global.websocket.destination.RoomWebSocketDestinations;
import com.sisibibi.api.global.websocket.WebSocketEventEnvelope;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class RoomClosedWebSocketEventListenerTest {

    private final RealtimeEventPublisher realtimeEventPublisher =
            mock(RealtimeEventPublisher.class);
    private final RoomClosedEventListener listener =
            new RoomClosedEventListener(realtimeEventPublisher);

    @Test
    void handle_publishesRoomClosedEnvelopeToRoomEventTopic() {
        LocalDateTime closedAt = LocalDateTime.of(2026, 6, 18, 12, 0);
        RoomClosedEvent event = new RoomClosedEvent(1L, closedAt);
        ArgumentCaptor<WebSocketEventEnvelope> envelopeCaptor =
                ArgumentCaptor.forClass(WebSocketEventEnvelope.class);

        listener.handle(event);

        verify(realtimeEventPublisher).publish(
                org.mockito.ArgumentMatchers.eq(RoomWebSocketDestinations.roomEvents(1L)),
                envelopeCaptor.capture()
        );
        WebSocketEventEnvelope envelope = envelopeCaptor.getValue();
        assertThat(envelope.eventType()).isEqualTo(RoomEventType.ROOM_CLOSED.name());
        assertThat(envelope.roomId()).isEqualTo(1L);
        assertThat(envelope.data()).isInstanceOf(RoomClosedEventPayload.class);
        RoomClosedEventPayload payload = (RoomClosedEventPayload) envelope.data();
        assertThat(payload.roomId()).isEqualTo(1L);
        assertThat(payload.status()).isEqualTo(RoomStatus.CLOSED);
        assertThat(payload.closedAt()).isEqualTo(closedAt);
    }

    @Test
    void handle_doesNotThrow_whenWebSocketPublishFails() {
        RoomClosedEvent event = new RoomClosedEvent(
                1L,
                LocalDateTime.of(2026, 6, 18, 12, 0)
        );
        org.mockito.BDDMockito.willThrow(new RuntimeException("broker down"))
                .given(realtimeEventPublisher)
                .publish(
                        org.mockito.ArgumentMatchers.eq(RoomWebSocketDestinations.roomEvents(1L)),
                        org.mockito.ArgumentMatchers.any()
                );

        listener.handle(event);
    }
}
