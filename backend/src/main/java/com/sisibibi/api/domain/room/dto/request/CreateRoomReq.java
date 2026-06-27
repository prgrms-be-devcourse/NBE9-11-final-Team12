package com.sisibibi.api.domain.room.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateRoomReq(
    @NotNull
    @Positive
    Long topicId,

    @NotBlank
    @Size(max = 100)
    String title,

    @Positive
    Integer maxParticipants
) {
}