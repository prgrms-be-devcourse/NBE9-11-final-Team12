package com.sisibibi.api.domain.room.dto.event;

public record RoomClosedMessage(
    String type,
    Long roomId,
    String message
) {

  public static RoomClosedMessage of(Long roomId) {
    return new RoomClosedMessage(
        "ROOM_CLOSED",
        roomId,
        "토론이 종료되었습니다."
    );
  }
}