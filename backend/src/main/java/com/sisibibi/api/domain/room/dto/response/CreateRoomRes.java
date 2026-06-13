package com.sisibibi.api.domain.room.dto.response;

import com.sisibibi.api.domain.room.entity.Room;
import com.sisibibi.api.domain.room.entity.RoomStatus;

import java.time.LocalDateTime;

public record CreateRoomRes(
    Long roomId,
    Long topicId,
    String title,
    RoomStatus status,
    LocalDateTime startedAt,
    LocalDateTime createdAt
) {

  public static CreateRoomRes from(Room room) {
    return new CreateRoomRes(
        room.getId(),
        room.getTopicId(),
        room.getTitle(),
        room.getStatus(),
        room.getStartedAt(),
        room.getCreatedAt()
    );
  }
}
