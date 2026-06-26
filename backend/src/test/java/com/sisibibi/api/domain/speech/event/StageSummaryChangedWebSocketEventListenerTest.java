package com.sisibibi.api.domain.speech.event;

import com.sisibibi.api.domain.speech.dto.event.StageSummaryChangedEvent;
import com.sisibibi.api.domain.speech.dto.event.StageSummaryEventPayload;
import com.sisibibi.api.domain.speech.dto.event.StageSummaryEventType;
import com.sisibibi.api.global.realtime.RealtimeEventPublisher;
import com.sisibibi.api.global.websocket.RoomWebSocketDestinations;
import com.sisibibi.api.global.websocket.WebSocketEventEnvelope;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class StageSummaryChangedWebSocketEventListenerTest {

    private final RealtimeEventPublisher realtimeEventPublisher =
            mock(RealtimeEventPublisher.class);
    private final StageSummaryChangedWebSocketEventListener listener =
            new StageSummaryChangedWebSocketEventListener(realtimeEventPublisher);

    @Test
    void handle_publishesStageSummaryEnvelopeToStageSummaryTopic() {
        StageSummaryEventPayload payload = new StageSummaryEventPayload(
                77L,
                1L
        );
        StageSummaryChangedEvent event = new StageSummaryChangedEvent(
                StageSummaryEventType.STAGE_SUMMARY_COMPLETED,
                1L,
                payload
        );
        ArgumentCaptor<WebSocketEventEnvelope> envelopeCaptor =
                ArgumentCaptor.forClass(WebSocketEventEnvelope.class);

        listener.handle(event);

        verify(realtimeEventPublisher).publish(
                org.mockito.ArgumentMatchers.eq(RoomWebSocketDestinations.stageSummaryEvents(1L)),
                envelopeCaptor.capture()
        );
        WebSocketEventEnvelope envelope = envelopeCaptor.getValue();
        assertThat(envelope.eventType())
                .isEqualTo(StageSummaryEventType.STAGE_SUMMARY_COMPLETED.name());
        assertThat(envelope.roomId()).isEqualTo(1L);
        assertThat(envelope.data()).isEqualTo(payload);
    }

    @Test
    void handle_doesNotThrow_whenWebSocketPublishFails() {
        StageSummaryEventPayload payload = new StageSummaryEventPayload(
                77L,
                1L
        );
        StageSummaryChangedEvent event = new StageSummaryChangedEvent(
                StageSummaryEventType.STAGE_SUMMARY_COMPLETED,
                1L,
                payload
        );
        org.mockito.BDDMockito.willThrow(new RuntimeException("broker down"))
                .given(realtimeEventPublisher)
                .publish(
                        org.mockito.ArgumentMatchers.eq(
                                RoomWebSocketDestinations.stageSummaryEvents(1L)
                        ),
                        org.mockito.ArgumentMatchers.any()
                );

        listener.handle(event);
    }
}
