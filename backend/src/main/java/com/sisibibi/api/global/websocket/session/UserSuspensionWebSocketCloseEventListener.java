package com.sisibibi.api.global.websocket.session;

import com.sisibibi.api.domain.usersanction.dto.event.UserSanctionChangedEvent;
import com.sisibibi.api.domain.usersanction.dto.event.UserSanctionEventType;
import com.sisibibi.api.domain.usersanction.entity.UserSanctionType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.socket.CloseStatus;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserSuspensionWebSocketCloseEventListener {

    private static final CloseStatus ACCOUNT_SUSPENDED_CLOSE_STATUS =
            new CloseStatus(CloseStatus.POLICY_VIOLATION.getCode(), "ACCOUNT_SUSPENDED");

    private final WebSocketSessionRegistry sessionRegistry;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(UserSanctionChangedEvent event) {
        if (event.type() != UserSanctionEventType.SANCTION_CREATED
                || event.payload().type() != UserSanctionType.ACCOUNT_SUSPENSION) {
            return;
        }

        int closedCount = sessionRegistry.closeUserSessions(
                event.userId(),
                ACCOUNT_SUSPENDED_CLOSE_STATUS
        );
        if (closedCount > 0) {
            log.info("Closed WebSocket sessions after account suspension. userId={}, closedCount={}",
                    event.userId(),
                    closedCount);
        }
    }
}
