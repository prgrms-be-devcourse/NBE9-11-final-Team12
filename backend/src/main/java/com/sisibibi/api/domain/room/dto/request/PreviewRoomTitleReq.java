package com.sisibibi.api.domain.room.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record PreviewRoomTitleReq(
    @NotNull
    @Positive
    Long topicId
) {
}