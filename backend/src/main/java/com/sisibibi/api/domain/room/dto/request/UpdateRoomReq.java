package com.sisibibi.api.domain.room.dto.request;

import java.time.LocalDateTime;

public record UpdateRoomReq(
    String title,
    LocalDateTime startedAt,
    LocalDateTime endedAt,
    Integer maxParticipants
) {
}