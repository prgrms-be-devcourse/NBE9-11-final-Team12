package com.sisibibi.api.domain.roomparticipant.dto.response;

public record RoomParticipantCountRes(
    Long roomId,
    long participantCount
) {
}