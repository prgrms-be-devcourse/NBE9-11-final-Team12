package com.sisibibi.api.domain.speech.event;

import com.sisibibi.api.domain.speech.dto.event.AiCounterIssueChangedEvent;
import com.sisibibi.api.domain.speech.dto.event.AiCounterIssueEventPayload;
import com.sisibibi.api.domain.speech.dto.event.AiCounterIssueEventType;
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

class AiCounterIssueChangedWebSocketEventListenerTest {

    private final RealtimeEventPublisher realtimeEventPublisher =
            mock(RealtimeEventPublisher.class);
    private final AiCounterIssueChangedWebSocketEventListener listener =
            new AiCounterIssueChangedWebSocketEventListener(realtimeEventPublisher);

    @Test
    void handle_publishesAiCounterIssueEnvelopeToAiCounterIssueEventTopic() {
        AiCounterIssueEventPayload payload = new AiCounterIssueEventPayload(
                11L,
                1L,
                30L,
                SpeechStance.CON,
                "반대 측에서 검토할 핵심 쟁점입니다.",
                LocalDateTime.of(2026, 6, 25, 14, 0),
                LocalDateTime.of(2026, 6, 25, 14, 1),
                LocalDateTime.of(2026, 6, 25, 14, 1)
        );
        AiCounterIssueChangedEvent event = new AiCounterIssueChangedEvent(
                AiCounterIssueEventType.AI_COUNTER_ISSUE_SUGGESTED,
                1L,
                payload
        );
        ArgumentCaptor<WebSocketEventEnvelope> envelopeCaptor =
                ArgumentCaptor.forClass(WebSocketEventEnvelope.class);

        listener.handle(event);

        verify(realtimeEventPublisher).publish(
                org.mockito.ArgumentMatchers.eq(
                        RoomWebSocketDestinations.aiCounterIssueEvents(1L)
                ),
                envelopeCaptor.capture()
        );
        WebSocketEventEnvelope envelope = envelopeCaptor.getValue();
        assertThat(envelope.eventType())
                .isEqualTo(AiCounterIssueEventType.AI_COUNTER_ISSUE_SUGGESTED.name());
        assertThat(envelope.roomId()).isEqualTo(1L);
        assertThat(envelope.data()).isEqualTo(payload);
    }

    @Test
    void handle_doesNotThrow_whenWebSocketPublishFails() {
        AiCounterIssueEventPayload payload = new AiCounterIssueEventPayload(
                11L,
                1L,
                30L,
                SpeechStance.CON,
                "반대 측에서 검토할 핵심 쟁점입니다.",
                LocalDateTime.of(2026, 6, 25, 14, 0),
                LocalDateTime.of(2026, 6, 25, 14, 1),
                LocalDateTime.of(2026, 6, 25, 14, 1)
        );
        AiCounterIssueChangedEvent event = new AiCounterIssueChangedEvent(
                AiCounterIssueEventType.AI_COUNTER_ISSUE_SUGGESTED,
                1L,
                payload
        );
        org.mockito.BDDMockito.willThrow(new RuntimeException("broker down"))
                .given(realtimeEventPublisher)
                .publish(
                        org.mockito.ArgumentMatchers.eq(
                                RoomWebSocketDestinations.aiCounterIssueEvents(1L)
                        ),
                        org.mockito.ArgumentMatchers.any()
                );

        listener.handle(event);
    }
}
