package com.sisibibi.api.domain.speech.dto.event;

public record StageChangedEvent(
        StageEventType type,
        Long roomId,
        StageEventPayload payload
) {
}
