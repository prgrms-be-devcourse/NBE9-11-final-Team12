package com.sisibibi.api.domain.roomparticipant.dto.response;

import com.sisibibi.api.domain.roomparticipant.entity.RoomParticipant;
import com.sisibibi.api.domain.roomparticipant.entity.RoomParticipantStatus;

import java.time.LocalDateTime;

public record RoomParticipantRes(
    Long roomParticipantId,
    Long roomId,
    Long userId,
    RoomParticipantStatus status,
    LocalDateTime joinedAt
) {

  public static RoomParticipantRes from(RoomParticipant participant) {
    return new RoomParticipantRes(
        participant.getId(),
        participant.getRoomId(),
        participant.getUserId(),
        participant.getStatus(),
        participant.getJoinedAt()
    );
  }
}