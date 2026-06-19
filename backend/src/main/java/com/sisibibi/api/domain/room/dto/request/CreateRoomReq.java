package com.sisibibi.api.domain.room.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateRoomReq(
    @NotNull
    @Positive
    Long topicId,

    @Positive
    Integer maxParticipants
) {
}
