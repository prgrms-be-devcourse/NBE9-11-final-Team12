package com.sisibibi.api.global.websocket.session;

import java.util.Set;

public record WebSocketSessionState(
        String sessionId,
        Long userId,
        Set<Long> roomIds
) {
}
