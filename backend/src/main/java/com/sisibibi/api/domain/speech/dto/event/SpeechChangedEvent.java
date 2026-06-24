package com.sisibibi.api.domain.speech.dto.event;

public record SpeechChangedEvent(
        SpeechEventType type,
        Long roomId,
        SpeechEventPayload payload
) {
}
