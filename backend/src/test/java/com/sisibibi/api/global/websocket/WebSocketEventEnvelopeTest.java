package com.sisibibi.api.global.websocket;

import com.sisibibi.api.domain.room.dto.event.RoomEventType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WebSocketEventEnvelopeTest {

    @Test
    void of_createsEnvelopeWithCommonMetadata() {
        WebSocketEventEnvelope<String> envelope = WebSocketEventEnvelope.of(
                RoomEventType.ROOM_CLOSED,
                1L,
                "payload"
        );

        assertThat(envelope.eventId()).isNotBlank();
        assertThat(envelope.eventType()).isEqualTo("ROOM_CLOSED");
        assertThat(envelope.roomId()).isEqualTo(1L);
        assertThat(envelope.data()).isEqualTo("payload");
        assertThat(envelope.occurredAt()).isNotNull();
    }
}
