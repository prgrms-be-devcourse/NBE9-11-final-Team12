package com.sisibibi.api.domain.roomparticipant.dto.event;

import java.time.LocalDateTime;

public record RoomParticipantEventPayload(
        Long roomId,
        Long userId,
        int participantCount,
        LocalDateTime occurredAt
) {

    public static RoomParticipantEventPayload of(
            Long roomId,
            Long userId,
            int participantCount
    ) {
        return new RoomParticipantEventPayload(
                roomId,
                userId,
                participantCount,
                LocalDateTime.now()
        );
    }
}
