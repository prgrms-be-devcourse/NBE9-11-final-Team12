package com.sisibibi.api.domain.speechreaction.dto.event;

public record SpeechReactionChangedEvent(
        SpeechReactionEventType type,
        Long roomId,
        SpeechReactionEventPayload payload
) {
}
