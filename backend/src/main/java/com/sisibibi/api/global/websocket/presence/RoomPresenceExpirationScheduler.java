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
public class RoomPresenceExpirationScheduler {

    private final RoomPresenceExpirationService roomPresenceExpirationService;

    @Scheduled(fixedDelayString = "${app.websocket.presence.expiration-fixed-delay-ms:5000}")
    public void expireDisconnectedParticipants() {
        roomPresenceExpirationService.expireDisconnectedParticipants();
    }
}
