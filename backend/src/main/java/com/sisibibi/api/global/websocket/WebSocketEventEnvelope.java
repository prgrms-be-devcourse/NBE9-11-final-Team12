package com.sisibibi.api.global.websocket;

import java.time.LocalDateTime;
import java.util.UUID;

public record WebSocketEventEnvelope<T>(
        String eventId,
        String eventType,
        Long roomId,
        T data,
        LocalDateTime occurredAt
) {

    public static <T> WebSocketEventEnvelope<T> of(
            Enum<?> eventType,
            Long roomId,
            T data
    ) {
        return new WebSocketEventEnvelope<>(
                UUID.randomUUID().toString(),
                eventType.name(),
                roomId,
                data,
                LocalDateTime.now()
        );
    }
}
