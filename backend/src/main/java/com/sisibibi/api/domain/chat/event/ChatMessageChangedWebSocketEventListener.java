package com.sisibibi.api.domain.chat.event;

import com.sisibibi.api.domain.chat.dto.event.ChatMessageChangedEvent;
import com.sisibibi.api.global.realtime.RealtimeEventPublisher;
import com.sisibibi.api.global.websocket.destination.RoomWebSocketDestinations;
import com.sisibibi.api.global.websocket.WebSocketEventEnvelope;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatMessageChangedWebSocketEventListener {

    private final RealtimeEventPublisher realtimeEventPublisher;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(ChatMessageChangedEvent event) {
        try {
            realtimeEventPublisher.publish(
                    RoomWebSocketDestinations.chatEvents(event.roomId()),
                    WebSocketEventEnvelope.of(
                            event.type(),
                            event.roomId(),
                            event.payload()
                    )
            );
        } catch (RuntimeException publishException) {
            log.warn(
                    "Failed to publish chat WebSocket event. roomId={}, type={}",
                    event.roomId(),
                    event.type(),
                    publishException
            );
        }
    }
}
