package com.sisibibi.api.global.websocket;

import com.sisibibi.api.domain.roomparticipant.entity.RoomParticipantStatus;
import com.sisibibi.api.domain.roomparticipant.repository.RoomParticipantRepository;
import com.sisibibi.api.global.exception.ErrorCode;
import com.sisibibi.api.global.security.AuthPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketAuthChannelInterceptor implements ChannelInterceptor {

    private final RoomParticipantRepository roomParticipantRepository;
    private final RedisWebSocketSessionRegistry sessionRegistry;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        StompCommand command = accessor.getCommand();

        if (command == null) {
            touchSession(accessor);
            return message;
        }

        if (command == StompCommand.CONNECT) {
            AuthPrincipal principal = authenticateConnect(accessor);
            registerSession(accessor, principal);
            return MessageBuilder.createMessage(message.getPayload(), accessor.getMessageHeaders());
        }

        if (command == StompCommand.SEND) {
            requirePrincipal(accessor);
            touchSession(accessor);
            return MessageBuilder.createMessage(message.getPayload(), accessor.getMessageHeaders());
        }

        if (command == StompCommand.SUBSCRIBE) {
            AuthPrincipal principal = requirePrincipal(accessor);
            Optional<Long> roomId = validateDestinationAccess(principal, accessor.getDestination());
            roomId.ifPresent(id -> registerRoomSession(accessor, principal, id));
            return MessageBuilder.createMessage(message.getPayload(), accessor.getMessageHeaders());
        }

        if (command == StompCommand.DISCONNECT) {
            unregisterSession(accessor);
            return message;
        }

        return message;
    }

    private AuthPrincipal authenticateConnect(StompHeaderAccessor accessor) {
        AuthPrincipal principal = resolveSessionPrincipal(accessor)
                .orElseThrow(() -> new AccessDeniedException(ErrorCode.UNAUTHORIZED.name()));

        accessor.setUser(toAuthentication(principal));
        return principal;
    }

    private AuthPrincipal requirePrincipal(StompHeaderAccessor accessor) {
        return resolvePrincipal(accessor)
                .orElseThrow(() -> new AccessDeniedException(ErrorCode.UNAUTHORIZED.name()));
    }

    private Optional<AuthPrincipal> resolvePrincipal(StompHeaderAccessor accessor) {
        Principal user = accessor.getUser();
        if (user instanceof Authentication authentication
                && authentication.getPrincipal() instanceof AuthPrincipal principal) {
            return Optional.of(principal);
        }

        return resolveSessionPrincipal(accessor);
    }

    private Optional<AuthPrincipal> resolveSessionPrincipal(StompHeaderAccessor accessor) {
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

    private UsernamePasswordAuthenticationToken toAuthentication(AuthPrincipal principal) {
        return new UsernamePasswordAuthenticationToken(
                principal,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + principal.role()))
        );
    }

    private Optional<Long> validateDestinationAccess(AuthPrincipal principal, String destination) {
        if (destination == null) {
            return Optional.empty();
        }

        Optional<Long> sanctionEventUserId =
                UserWebSocketDestinations.findSanctionEventUserId(destination);
        if (sanctionEventUserId.isPresent()) {
            if (!sanctionEventUserId.get().equals(principal.userId())) {
                throw new AccessDeniedException(ErrorCode.FORBIDDEN.name());
            }
            return Optional.empty();
        }
        if (UserWebSocketDestinations.isUserTopic(destination)) {
            throw new AccessDeniedException(ErrorCode.FORBIDDEN.name());
        }

        Optional<Long> allowedRoomId =
                RoomWebSocketDestinations.findAllowedRoomTopicId(destination);
        if (allowedRoomId.isEmpty()) {
            if (RoomWebSocketDestinations.isRoomTopic(destination)) {
                throw new AccessDeniedException(ErrorCode.FORBIDDEN.name());
            }
            return Optional.empty();
        }

        boolean joined = roomParticipantRepository.existsByRoomIdAndUserIdAndStatus(
                allowedRoomId.get(),
                principal.userId(),
                RoomParticipantStatus.JOINED
        );

        if (!joined) {
            throw new AccessDeniedException(ErrorCode.ROOM_PARTICIPATION_REQUIRED.name());
        }

        return allowedRoomId;
    }

    private void registerSession(StompHeaderAccessor accessor, AuthPrincipal principal) {
        String sessionId = accessor.getSessionId();
        if (sessionId == null) {
            return;
        }

        try {
            sessionRegistry.registerSession(sessionId, principal.userId());
        } catch (RuntimeException e) {
            log.warn(
                    "Failed to register WebSocket session in Redis. sessionId={}, userId={}",
                    sessionId,
                    principal.userId(),
                    e
            );
        }
    }

    private void registerRoomSession(
            StompHeaderAccessor accessor,
            AuthPrincipal principal,
            Long roomId
    ) {
        String sessionId = accessor.getSessionId();
        if (sessionId == null) {
            return;
        }

        try {
            sessionRegistry.registerRoomSession(sessionId, roomId, principal.userId());
        } catch (RuntimeException e) {
            log.warn(
                    "Failed to register WebSocket room session in Redis. sessionId={}, roomId={}, userId={}",
                    sessionId,
                    roomId,
                    principal.userId(),
                    e
            );
        }
    }

    private void touchSession(StompHeaderAccessor accessor) {
        String sessionId = accessor.getSessionId();
        if (sessionId == null) {
            return;
        }

        try {
            sessionRegistry.touchSession(sessionId);
        } catch (RuntimeException e) {
            log.warn("Failed to touch WebSocket session in Redis. sessionId={}", sessionId, e);
        }
    }

    private void unregisterSession(StompHeaderAccessor accessor) {
        String sessionId = accessor.getSessionId();
        if (sessionId == null) {
            return;
        }

        try {
            sessionRegistry.unregisterSession(sessionId);
        } catch (RuntimeException e) {
            log.warn("Failed to unregister WebSocket session in Redis. sessionId={}", sessionId, e);
        }
    }
}
