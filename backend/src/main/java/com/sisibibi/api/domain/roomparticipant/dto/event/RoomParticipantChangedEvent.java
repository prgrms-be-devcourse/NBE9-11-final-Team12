package com.sisibibi.api.domain.roomparticipant.dto.event;

public record RoomParticipantChangedEvent(
        RoomParticipantEventType type,
        Long roomId,
        RoomParticipantEventPayload payload
) {
}
