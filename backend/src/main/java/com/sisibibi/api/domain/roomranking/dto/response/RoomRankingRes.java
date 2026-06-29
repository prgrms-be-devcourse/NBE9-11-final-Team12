package com.sisibibi.api.domain.roomranking.dto.response;

import com.sisibibi.api.domain.room.dto.response.RoomSummaryRes;

public record RoomRankingRes(
    Long rank,
    Double score,
    RoomSummaryRes room
) {
}