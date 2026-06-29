package com.sisibibi.api.domain.usersanction.event;

import com.sisibibi.api.domain.usersanction.dto.event.UserSanctionChangedEvent;
import com.sisibibi.api.domain.usersanction.dto.event.UserSanctionEventPayload;
import com.sisibibi.api.domain.usersanction.dto.event.UserSanctionEventType;
import com.sisibibi.api.domain.usersanction.entity.UserSanctionState;
import com.sisibibi.api.domain.usersanction.entity.UserSanctionType;
import com.sisibibi.api.global.realtime.RealtimeEventPublisher;
import com.sisibibi.api.global.websocket.destination.UserWebSocketDestinations;
import com.sisibibi.api.global.websocket.WebSocketEventEnvelope;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;

class UserSanctionChangedWebSocketEventListenerTest {

    private final RealtimeEventPublisher realtimeEventPublisher =
            mock(RealtimeEventPublisher.class);
    private final UserSanctionChangedWebSocketEventListener listener =
            new UserSanctionChangedWebSocketEventListener(realtimeEventPublisher);

    @Test
    void handle_publishesSanctionEventToTargetUserTopic() {
        UserSanctionChangedEvent event = event();
        ArgumentCaptor<WebSocketEventEnvelope<?>> envelopeCaptor =
                ArgumentCaptor.forClass(WebSocketEventEnvelope.class);

        listener.handle(event);

        verify(realtimeEventPublisher).publish(
                eq(UserWebSocketDestinations.sanctionEvents(10L)),
                envelopeCaptor.capture()
        );
        assertThat(envelopeCaptor.getValue().eventType()).isEqualTo("SANCTION_CREATED");
        assertThat(envelopeCaptor.getValue().roomId()).isNull();
        assertThat(envelopeCaptor.getValue().data()).isEqualTo(event.payload());
    }

    @Test
    void handle_doesNotThrow_whenWebSocketPublishFails() {
        UserSanctionChangedEvent event = event();
        willThrow(new RuntimeException("broker down"))
                .given(realtimeEventPublisher)
                .publish(
                        eq(UserWebSocketDestinations.sanctionEvents(10L)),
                        any()
                );

        assertThatCode(() -> listener.handle(event)).doesNotThrowAnyException();
    }

    @Test
    void handle_doesNotPublish_whenAccountSuspensionIsRevoked() {
        LocalDateTime startsAt = LocalDateTime.of(2026, 6, 22, 12, 0);
        UserSanctionChangedEvent event = new UserSanctionChangedEvent(
                UserSanctionEventType.SANCTION_REVOKED,
                10L,
                new UserSanctionEventPayload(
                        201L,
                        UserSanctionType.ACCOUNT_SUSPENSION,
                        "반복적인 운영 정책 위반",
                        UserSanctionState.REVOKED,
                        startsAt,
                        null
                )
        );

        listener.handle(event);

        verifyNoInteractions(realtimeEventPublisher);
    }

    @Test
    void handle_publishesAccountSuspensionCreated_whenUserMayStillBeConnected() {
        LocalDateTime startsAt = LocalDateTime.of(2026, 6, 22, 12, 0);
        UserSanctionChangedEvent event = new UserSanctionChangedEvent(
                UserSanctionEventType.SANCTION_CREATED,
                10L,
                new UserSanctionEventPayload(
                        201L,
                        UserSanctionType.ACCOUNT_SUSPENSION,
                        "반복적인 운영 정책 위반",
                        UserSanctionState.ACTIVE,
                        startsAt,
                        null
                )
        );

        listener.handle(event);

        verify(realtimeEventPublisher).publish(
                eq(UserWebSocketDestinations.sanctionEvents(10L)),
                any()
        );
    }

    private UserSanctionChangedEvent event() {
        LocalDateTime startsAt = LocalDateTime.of(2026, 6, 22, 12, 0);
        return new UserSanctionChangedEvent(
                UserSanctionEventType.SANCTION_CREATED,
                10L,
                new UserSanctionEventPayload(
                        200L,
                        UserSanctionType.CHAT_RESTRICTION,
                        "반복적인 채팅 도배",
                        UserSanctionState.ACTIVE,
                        startsAt,
                        startsAt.plusHours(24)
                )
        );
    }
}
