package com.sisibibi.api.domain.chat.dto.event;

public record ChatMessageChangedEvent(
        ChatEventType type,
        Long roomId,
        ChatMessageEventPayload payload
) {
}
