package com.sisibibi.api.global.websocket.session;

import com.sisibibi.api.global.security.AuthPrincipal;
import com.sisibibi.api.global.websocket.auth.WebSocketAuthAttributes;
import com.sisibibi.api.global.websocket.destination.RoomWebSocketDestinations;
import com.sisibibi.api.global.websocket.presence.RoomPresenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

import java.security.Principal;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketSessionRegistryEventListener {

    private final WebSocketSessionRegistry sessionRegistry;
    private final RoomPresenceService roomPresenceService;

    @EventListener
    public void handleSessionConnect(SessionConnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();
        if (sessionId == null) {
            return;
        }

        resolvePrincipal(accessor).ifPresent(principal ->
                sessionRegistry.bindUser(sessionId, principal.userId())
        );
    }

    @EventListener
    public void handleSessionSubscribe(SessionSubscribeEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();
        if (sessionId == null) {
            return;
        }

        Optional<Long> roomId = RoomWebSocketDestinations.findAllowedRoomTopicId(
                accessor.getDestination()
        );
        if (roomId.isEmpty()) {
            return;
        }

        resolvePrincipal(accessor).ifPresent(principal ->
                bindRoomSession(sessionId, principal.userId(), roomId.get())
        );
    }

    @EventListener
    public void handleSessionDisconnect(SessionDisconnectEvent event) {
        WebSocketSessionState state = sessionRegistry.unregisterSessionState(event.getSessionId());
        if (state.userId() == null) {
            return;
        }

        for (Long roomId : state.roomIds()) {
            if (sessionRegistry.findSessionIdsByRoomAndUser(roomId, state.userId()).isEmpty()) {
                roomPresenceService.recordDisconnected(roomId, state.userId(), state.sessionId());
            }
        }

        log.debug(
                "WebSocket session disconnected. sessionId={}, userId={}, roomIds={}",
                state.sessionId(),
                state.userId(),
                state.roomIds()
        );
    }

    private void bindRoomSession(String sessionId, Long userId, Long roomId) {
        sessionRegistry.bindRoom(sessionId, userId, roomId);
        roomPresenceService.recordConnected(roomId, userId, sessionId);
    }

    private Optional<AuthPrincipal> resolvePrincipal(StompHeaderAccessor accessor) {
        Principal user = accessor.getUser();
        if (user instanceof Authentication authentication
                && authentication.getPrincipal() instanceof AuthPrincipal principal) {
            return Optional.of(principal);
        }

        Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
        if (sessionAttributes == null) {
            return Optional.empty();
        }

        Object principal = sessionAttributes.get(WebSocketAuthAttributes.AUTH_PRINCIPAL);
        if (principal instanceof AuthPrincipal authPrincipal) {
            return Optional.of(authPrincipal);
        }

        return Optional.empty();
    }
}
