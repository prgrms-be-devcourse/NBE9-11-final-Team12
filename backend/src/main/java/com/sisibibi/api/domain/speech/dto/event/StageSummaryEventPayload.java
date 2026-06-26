package com.sisibibi.api.domain.speech.dto.event;

import com.sisibibi.api.domain.speech.entity.StageSummary;

public record StageSummaryEventPayload(
        Long summaryId,
        Long roomId
) {

    public static StageSummaryEventPayload from(StageSummary summary) {
        return new StageSummaryEventPayload(
                summary.getId(),
                summary.getRoomId()
        );
    }
}
