package com.sisibibi.api.global.websocket;

import com.sisibibi.api.domain.room.dto.event.RoomClosedEvent;
import com.sisibibi.api.domain.roomparticipant.dto.event.RoomParticipantChangedEvent;
import com.sisibibi.api.domain.roomparticipant.dto.event.RoomParticipantEventType;
import com.sisibibi.api.global.config.AsyncConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class RoomPresenceEventListener {

    private final RoomPresenceService roomPresenceService;

    @Async(AsyncConfig.DOMAIN_EVENT_TASK_EXECUTOR)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(RoomParticipantChangedEvent event) {
        if (event.type() != RoomParticipantEventType.PARTICIPANT_LEFT) {
            return;
        }

        roomPresenceService.clearPresence(event.roomId(), event.payload().userId());
    }

    @Async(AsyncConfig.DOMAIN_EVENT_TASK_EXECUTOR)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(RoomClosedEvent event) {
        roomPresenceService.clearRoomPresence(event.roomId());
    }
}
