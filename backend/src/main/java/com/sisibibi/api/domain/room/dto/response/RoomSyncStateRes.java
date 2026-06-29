package com.sisibibi.api.domain.room.dto.response;

import com.sisibibi.api.domain.room.entity.RoomStatus;

public record RoomSyncStateRes(
    Long roomId,
    RoomStatus roomStatus,
    String myParticipantStatus,
    int participantCount,
    boolean canJoin,
    boolean canSubscribe,
    boolean canWrite,
    String readMode
) {
}
