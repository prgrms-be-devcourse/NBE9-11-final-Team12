package com.sisibibi.api.domain.speech.dto.event;

public record AiCounterIssueChangedEvent(
        AiCounterIssueEventType type,
        Long roomId,
        AiCounterIssueEventPayload payload
) {
}
