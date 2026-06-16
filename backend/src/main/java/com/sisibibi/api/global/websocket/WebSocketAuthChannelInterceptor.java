package com.sisibibi.api.global.websocket;

import com.sisibibi.api.domain.roomparticipant.entity.RoomParticipantStatus;
import com.sisibibi.api.domain.roomparticipant.repository.RoomParticipantRepository;
import com.sisibibi.api.global.exception.ErrorCode;
import com.sisibibi.api.global.security.AuthPrincipal;
import lombok.RequiredArgsConstructor;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class WebSocketAuthChannelInterceptor implements ChannelInterceptor {

    private static final Pattern CHAT_DESTINATION_PATTERN =
            Pattern.compile("^/(?:app|topic)/rooms/(\\d+)/chat/messages$");

    private final RoomParticipantRepository roomParticipantRepository;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        StompCommand command = accessor.getCommand();

        if (command == null) {
            return message;
        }

        if (command == StompCommand.CONNECT) {
            authenticateConnect(accessor);
            return MessageBuilder.createMessage(message.getPayload(), accessor.getMessageHeaders());
        }

        if (command == StompCommand.SEND) {
            requirePrincipal(accessor);
            return MessageBuilder.createMessage(message.getPayload(), accessor.getMessageHeaders());
        }

        if (command == StompCommand.SUBSCRIBE) {
            AuthPrincipal principal = requirePrincipal(accessor);
            validateChatDestinationAccess(principal, accessor.getDestination());
            return MessageBuilder.createMessage(message.getPayload(), accessor.getMessageHeaders());
        }

        return message;
    }

    private void authenticateConnect(StompHeaderAccessor accessor) {
        AuthPrincipal principal = resolveSessionPrincipal(accessor)
                .orElseThrow(() -> new AccessDeniedException(ErrorCode.UNAUTHORIZED.name()));

        accessor.setUser(toAuthentication(principal));
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

    private void validateChatDestinationAccess(AuthPrincipal principal, String destination) {
        if (destination == null) {
            return;
        }

        Matcher matcher = CHAT_DESTINATION_PATTERN.matcher(destination);
        if (!matcher.matches()) {
            return;
        }

        Long roomId = Long.valueOf(matcher.group(1));
        boolean joined = roomParticipantRepository.existsByRoomIdAndUserIdAndStatus(
                roomId,
                principal.userId(),
                RoomParticipantStatus.JOINED
        );

        if (!joined) {
            throw new AccessDeniedException(ErrorCode.ROOM_PARTICIPATION_REQUIRED.name());
        }
    }
}
