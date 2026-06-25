package com.sisibibi.api.domain.speech.dto.event;

public record StageSummaryChangedEvent(
        StageSummaryEventType type,
        Long roomId,
        StageSummaryEventPayload payload
) {
}
