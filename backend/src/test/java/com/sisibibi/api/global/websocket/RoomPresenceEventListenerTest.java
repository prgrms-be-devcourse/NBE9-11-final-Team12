package com.sisibibi.api.global.websocket;

import com.sisibibi.api.domain.room.dto.event.RoomClosedEvent;
import com.sisibibi.api.domain.roomparticipant.dto.event.RoomParticipantChangedEvent;
import com.sisibibi.api.domain.roomparticipant.dto.event.RoomParticipantEventPayload;
import com.sisibibi.api.domain.roomparticipant.dto.event.RoomParticipantEventType;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class RoomPresenceEventListenerTest {

    private final RoomPresenceService roomPresenceService = mock(RoomPresenceService.class);
    private final RoomPresenceEventListener listener =
            new RoomPresenceEventListener(roomPresenceService);

    @Test
    void handle_clearsPresenceWhenParticipantLeft() {
        listener.handle(new RoomParticipantChangedEvent(
                RoomParticipantEventType.PARTICIPANT_LEFT,
                1L,
                RoomParticipantEventPayload.of(1L, 2L, 3)
        ));

        verify(roomPresenceService).clearPresence(1L, 2L);
    }

    @Test
    void handle_ignoresParticipantJoined() {
        listener.handle(new RoomParticipantChangedEvent(
                RoomParticipantEventType.PARTICIPANT_JOINED,
                1L,
                RoomParticipantEventPayload.of(1L, 2L, 3)
        ));

        verify(roomPresenceService, never())
                .clearPresence(
                        org.mockito.ArgumentMatchers.anyLong(),
                        org.mockito.ArgumentMatchers.anyLong()
                );
    }

    @Test
    void handle_clearsRoomPresenceWhenRoomClosed() {
        listener.handle(new RoomClosedEvent(
                1L,
                LocalDateTime.of(2026, 6, 28, 12, 0)
        ));

        verify(roomPresenceService).clearRoomPresence(1L);
    }
}
