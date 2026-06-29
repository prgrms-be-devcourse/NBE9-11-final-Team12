package com.sisibibi.api.domain.roomparticipant.event;

import com.sisibibi.api.domain.roomparticipant.dto.event.RoomParticipantChangedEvent;
import com.sisibibi.api.global.config.AsyncConfig;
import com.sisibibi.api.global.realtime.RealtimeEventPublisher;
import com.sisibibi.api.global.websocket.destination.RoomWebSocketDestinations;
import com.sisibibi.api.global.websocket.WebSocketEventEnvelope;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class RoomParticipantChangedWebSocketEventListener {

    private final RealtimeEventPublisher realtimeEventPublisher;

    @Async(AsyncConfig.DOMAIN_EVENT_TASK_EXECUTOR)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(RoomParticipantChangedEvent event) {
        try {
            realtimeEventPublisher.publish(
                    RoomWebSocketDestinations.participantEvents(event.roomId()),
                    WebSocketEventEnvelope.of(
                            event.type(),
                            event.roomId(),
                            event.payload()
                    )
            );
        } catch (RuntimeException publishException) {
            log.warn(
                    "Failed to publish room participant WebSocket event. roomId={}, type={}",
                    event.roomId(),
                    event.type(),
                    publishException
            );
        }
    }
}
