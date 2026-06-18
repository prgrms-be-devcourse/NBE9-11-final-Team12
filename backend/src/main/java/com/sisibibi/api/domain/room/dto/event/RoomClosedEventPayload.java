package com.sisibibi.api.domain.room.dto.event;

import com.sisibibi.api.domain.room.entity.Room;
import com.sisibibi.api.domain.room.entity.RoomStatus;
import java.time.LocalDateTime;

public record RoomClosedEventPayload(
        Long roomId,
        RoomStatus status,
        String message,
        LocalDateTime closedAt
) {

    public static RoomClosedEventPayload from(Room room) {
        return new RoomClosedEventPayload(
                room.getId(),
                room.getStatus(),
                "토론이 종료되었습니다.",
                room.getEndedAt()
        );
    }
}
