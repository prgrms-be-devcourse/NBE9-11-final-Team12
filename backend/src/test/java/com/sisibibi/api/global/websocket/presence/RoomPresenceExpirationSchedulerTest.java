package com.sisibibi.api.global.websocket.presence;

import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class RoomPresenceExpirationSchedulerTest {

    @Test
    void expireDisconnectedParticipants_delegatesToService() {
        RoomPresenceExpirationService service = mock(RoomPresenceExpirationService.class);
        RoomPresenceExpirationScheduler scheduler = new RoomPresenceExpirationScheduler(service);

        scheduler.expireDisconnectedParticipants();

        verify(service).expireDisconnectedParticipants();
    }
}
