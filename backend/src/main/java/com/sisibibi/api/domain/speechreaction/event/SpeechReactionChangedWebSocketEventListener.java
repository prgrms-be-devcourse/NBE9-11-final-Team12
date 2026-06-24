package com.sisibibi.api.domain.speechreaction.event;

import com.sisibibi.api.domain.speechreaction.dto.event.SpeechReactionChangedEvent;
import com.sisibibi.api.global.config.AsyncConfig;
import com.sisibibi.api.global.realtime.RealtimeEventPublisher;
import com.sisibibi.api.global.websocket.RoomWebSocketDestinations;
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
public class SpeechReactionChangedWebSocketEventListener {

    private final RealtimeEventPublisher realtimeEventPublisher;

    @Async(AsyncConfig.DOMAIN_EVENT_TASK_EXECUTOR)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(SpeechReactionChangedEvent event) {
        try {
            realtimeEventPublisher.publish(
                    RoomWebSocketDestinations.speechReactionEvents(event.roomId()),
                    WebSocketEventEnvelope.of(
                            event.type(),
                            event.roomId(),
                            event.payload()
                    )
            );
        } catch (RuntimeException publishException) {
            log.warn(
                    "Failed to publish speech reaction WebSocket event. roomId={}, speechId={}",
                    event.roomId(),
                    event.payload().speechId(),
                    publishException
            );
        }
    }
}
