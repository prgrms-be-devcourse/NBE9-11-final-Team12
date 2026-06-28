package com.sisibibi.api.global.websocket.presence;

import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class RoomPresenceCleanupSchedulerTest {

    @Test
    void cleanupExpiredDisconnectedPresence_delegatesToService() {
        RoomPresenceService service = mock(RoomPresenceService.class);
        RoomPresenceCleanupScheduler scheduler = new RoomPresenceCleanupScheduler(service);

        scheduler.cleanupExpiredDisconnectedPresence();

        verify(service).cleanupExpiredDisconnectedPresence();
    }
}
