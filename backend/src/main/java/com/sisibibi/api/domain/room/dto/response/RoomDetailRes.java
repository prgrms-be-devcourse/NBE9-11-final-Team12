package com.sisibibi.api.domain.room.dto.response;

import com.sisibibi.api.domain.room.entity.Room;
import com.sisibibi.api.domain.room.entity.RoomStatus;

import java.time.LocalDateTime;

public record RoomDetailRes(
    Long roomId,
    Long topicId,
    String title,
    RoomStatus status,
    LocalDateTime startedAt,
    LocalDateTime endedAt,
    LocalDateTime createdAt
) {

  public static RoomDetailRes from(Room room) {
    return new RoomDetailRes(
        room.getId(),
        room.getTopicId(),
        room.getTitle(),
        room.getStatus(),
        room.getStartedAt(),
        room.getEndedAt(),
        room.getCreatedAt()
    );
  }
}