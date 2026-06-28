package com.sisibibi.api.global.websocket.session;

import com.sisibibi.api.domain.usersanction.dto.event.UserSanctionChangedEvent;
import com.sisibibi.api.domain.usersanction.dto.event.UserSanctionEventPayload;
import com.sisibibi.api.domain.usersanction.dto.event.UserSanctionEventType;
import com.sisibibi.api.domain.usersanction.entity.UserSanctionState;
import com.sisibibi.api.domain.usersanction.entity.UserSanctionType;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.socket.CloseStatus;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class UserSuspensionWebSocketCloseEventListenerTest {

    private final WebSocketSessionRegistry sessionRegistry = mock(WebSocketSessionRegistry.class);
    private final UserSuspensionWebSocketCloseEventListener listener =
            new UserSuspensionWebSocketCloseEventListener(sessionRegistry);

    @Test
    void handle_closesUserSessionsWhenAccountSuspensionCreated() {
        listener.handle(event(
                UserSanctionEventType.SANCTION_CREATED,
                UserSanctionType.ACCOUNT_SUSPENSION
        ));

        ArgumentCaptor<CloseStatus> closeStatusCaptor =
                ArgumentCaptor.forClass(CloseStatus.class);
        verify(sessionRegistry).closeUserSessions(
                org.mockito.ArgumentMatchers.eq(2L),
                closeStatusCaptor.capture()
        );
        assertThat(closeStatusCaptor.getValue().getCode())
                .isEqualTo(CloseStatus.POLICY_VIOLATION.getCode());
        assertThat(closeStatusCaptor.getValue().getReason())
                .isEqualTo("ACCOUNT_SUSPENDED");
    }

    @Test
    void handle_ignoresSanctionRevoked() {
        listener.handle(event(
                UserSanctionEventType.SANCTION_REVOKED,
                UserSanctionType.ACCOUNT_SUSPENSION
        ));

        verify(sessionRegistry, never())
                .closeUserSessions(
                        org.mockito.ArgumentMatchers.anyLong(),
                        org.mockito.ArgumentMatchers.any()
                );
    }

    @Test
    void handle_ignoresNonAccountSuspension() {
        listener.handle(event(
                UserSanctionEventType.SANCTION_CREATED,
                UserSanctionType.SPEECH_RESTRICTION
        ));

        verify(sessionRegistry, never())
                .closeUserSessions(
                        org.mockito.ArgumentMatchers.anyLong(),
                        org.mockito.ArgumentMatchers.any()
                );
    }

    private UserSanctionChangedEvent event(
            UserSanctionEventType eventType,
            UserSanctionType sanctionType
    ) {
        return new UserSanctionChangedEvent(
                eventType,
                2L,
                new UserSanctionEventPayload(
                        1L,
                        sanctionType,
                        "reason",
                        UserSanctionState.ACTIVE,
                        LocalDateTime.of(2026, 6, 28, 12, 0),
                        null
                )
        );
    }
}
