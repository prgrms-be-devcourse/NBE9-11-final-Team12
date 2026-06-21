package com.sisibibi.api.domain.usersanction.event;

import com.sisibibi.api.domain.usersanction.dto.event.UserSanctionChangedEvent;
import com.sisibibi.api.global.websocket.UserWebSocketDestinations;
import com.sisibibi.api.global.websocket.WebSocketEventEnvelope;
import com.sisibibi.api.global.websocket.WebSocketEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserSanctionChangedWebSocketEventListener {

    private final WebSocketEventPublisher webSocketEventPublisher;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(UserSanctionChangedEvent event) {
        try {
            webSocketEventPublisher.publish(
                    UserWebSocketDestinations.sanctionEvents(event.userId()),
                    WebSocketEventEnvelope.of(
                            event.type(),
                            null,
                            event.payload()
                    )
            );
        } catch (RuntimeException publishException) {
            log.warn(
                    "Failed to publish user sanction WebSocket event. userId={}, type={}",
                    event.userId(),
                    event.type(),
                    publishException
            );
        }
    }
}
