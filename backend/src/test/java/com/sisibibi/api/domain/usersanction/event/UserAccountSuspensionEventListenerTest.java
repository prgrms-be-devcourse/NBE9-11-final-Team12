package com.sisibibi.api.domain.usersanction.event;

import com.sisibibi.api.domain.usersanction.dto.event.UserSanctionChangedEvent;
import com.sisibibi.api.domain.usersanction.dto.event.UserSanctionEventPayload;
import com.sisibibi.api.domain.usersanction.dto.event.UserSanctionEventType;
import com.sisibibi.api.domain.usersanction.entity.UserSanctionState;
import com.sisibibi.api.domain.usersanction.entity.UserSanctionType;
import com.sisibibi.api.global.security.refresh.RefreshTokenStore;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class UserAccountSuspensionEventListenerTest {

    private final RefreshTokenStore refreshTokenStore = mock(RefreshTokenStore.class);
    private final UserAccountSuspensionEventListener listener =
            new UserAccountSuspensionEventListener(refreshTokenStore);

    @Test
    void handle_marksBannedAndDeletesRefreshTokens_whenAccountIsSuspended() {
        listener.handle(event(UserSanctionEventType.SANCTION_CREATED));

        verify(refreshTokenStore).deleteAll(10L);
    }

    @Test
    void handle_doesNotDeleteRefreshTokens_whenAccountSuspensionIsRevoked() {
        listener.handle(event(UserSanctionEventType.SANCTION_REVOKED));

        verifyNoInteractions(refreshTokenStore);
    }

    @Test
    void handle_ignoresOtherSanctionTypes() {
        UserSanctionChangedEvent event = new UserSanctionChangedEvent(
                UserSanctionEventType.SANCTION_CREATED,
                10L,
                new UserSanctionEventPayload(
                        200L,
                        UserSanctionType.CHAT_RESTRICTION,
                        "채팅 제한",
                        UserSanctionState.ACTIVE,
                        LocalDateTime.now(),
                        LocalDateTime.now().plusHours(24)
                )
        );

        listener.handle(event);

        verifyNoInteractions(refreshTokenStore);
    }

    private UserSanctionChangedEvent event(UserSanctionEventType eventType) {
        return new UserSanctionChangedEvent(
                eventType,
                10L,
                new UserSanctionEventPayload(
                        201L,
                        UserSanctionType.ACCOUNT_SUSPENSION,
                        "반복적인 운영 정책 위반",
                        eventType == UserSanctionEventType.SANCTION_REVOKED
                                ? UserSanctionState.REVOKED
                                : UserSanctionState.ACTIVE,
                        LocalDateTime.now(),
                        null
                )
        );
    }
}
