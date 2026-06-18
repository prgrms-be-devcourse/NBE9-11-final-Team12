package com.sisibibi.api.domain.room.dto.event;

import java.time.LocalDateTime;

public record RoomClosedEvent(
        Long roomId,
        LocalDateTime closedAt
) {
}
