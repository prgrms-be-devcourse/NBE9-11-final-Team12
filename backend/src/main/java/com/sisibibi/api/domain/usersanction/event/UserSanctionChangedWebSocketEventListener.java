package com.sisibibi.api.domain.usersanction.event;

import com.sisibibi.api.domain.usersanction.dto.event.UserSanctionChangedEvent;
import com.sisibibi.api.domain.usersanction.dto.event.UserSanctionEventType;
import com.sisibibi.api.domain.usersanction.entity.UserSanctionType;
import com.sisibibi.api.global.realtime.RealtimeEventPublisher;
import com.sisibibi.api.global.websocket.destination.UserWebSocketDestinations;
import com.sisibibi.api.global.websocket.WebSocketEventEnvelope;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserSanctionChangedWebSocketEventListener {

    private final RealtimeEventPublisher realtimeEventPublisher;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(UserSanctionChangedEvent event) {
        if (isAccountSuspensionRevoked(event)) {
            return;
        }

        try {
            realtimeEventPublisher.publish(
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

    private boolean isAccountSuspensionRevoked(UserSanctionChangedEvent event) {
        return event.type() == UserSanctionEventType.SANCTION_REVOKED
                && event.payload().type() == UserSanctionType.ACCOUNT_SUSPENSION;
    }
}
