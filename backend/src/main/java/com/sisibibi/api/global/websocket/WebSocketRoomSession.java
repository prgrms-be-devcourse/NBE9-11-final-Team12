package com.sisibibi.api.global.websocket;

public record WebSocketRoomSession(
        Long roomId,
        Long userId,
        String sessionId
) {
}
