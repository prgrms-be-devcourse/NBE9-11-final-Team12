package com.sisibibi.api.global.websocket.presence;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "app.websocket.presence",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class RoomPresenceCleanupScheduler {

    private final RoomPresenceService roomPresenceService;

    @Scheduled(fixedDelayString = "${app.websocket.presence.cleanup-fixed-delay-ms:60000}")
    public void cleanupExpiredDisconnectedPresence() {
        roomPresenceService.cleanupExpiredDisconnectedPresence();
    }
}
