package com.sisibibi.api.domain.room.event;

import com.sisibibi.api.domain.room.dto.event.RoomClosedEvent;
import com.sisibibi.api.domain.room.dto.event.RoomClosedEventPayload;
import com.sisibibi.api.domain.room.dto.event.RoomEventType;
import com.sisibibi.api.global.realtime.RealtimeEventPublisher;
import com.sisibibi.api.global.config.AsyncConfig;
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
public class RoomClosedEventListener {

    private final RealtimeEventPublisher realtimeEventPublisher;

    @Async(AsyncConfig.DOMAIN_EVENT_TASK_EXECUTOR)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(RoomClosedEvent event) {
        try {
            realtimeEventPublisher.publish(
                    RoomWebSocketDestinations.roomEvents(event.roomId()),
                    WebSocketEventEnvelope.of(
                            RoomEventType.ROOM_CLOSED,
                            event.roomId(),
                            RoomClosedEventPayload.from(event)
                    )
            );
        } catch (RuntimeException publishException) {
            log.warn(
                    "Failed to publish room closed WebSocket event. roomId={}",
                    event.roomId(),
                    publishException
            );
        }
    }
}
