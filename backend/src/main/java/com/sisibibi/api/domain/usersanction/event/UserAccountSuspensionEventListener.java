package com.sisibibi.api.domain.usersanction.event;

import com.sisibibi.api.domain.usersanction.dto.event.UserSanctionChangedEvent;
import com.sisibibi.api.domain.usersanction.dto.event.UserSanctionEventType;
import com.sisibibi.api.domain.usersanction.entity.UserSanctionType;
import com.sisibibi.api.global.security.refresh.RefreshTokenStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserAccountSuspensionEventListener {

    private final RefreshTokenStore refreshTokenStore;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(UserSanctionChangedEvent event) {
        if (event.payload().type() != UserSanctionType.ACCOUNT_SUSPENSION) {
            return;
        }

        if (event.type() == UserSanctionEventType.SANCTION_CREATED) {
            deleteRefreshTokens(event.userId());
        }
    }

    private void deleteRefreshTokens(Long userId) {
        try {
            refreshTokenStore.deleteAll(userId);
        } catch (RuntimeException redisException) {
            log.warn(
                    "Failed to delete refresh tokens after account suspension. userId={}",
                    userId,
                    redisException
            );
        }
    }
}
