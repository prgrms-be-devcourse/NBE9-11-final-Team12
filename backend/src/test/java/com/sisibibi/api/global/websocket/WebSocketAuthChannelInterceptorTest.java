package com.sisibibi.api.global.websocket;

import com.sisibibi.api.domain.roomparticipant.entity.RoomParticipantStatus;
import com.sisibibi.api.domain.roomparticipant.repository.RoomParticipantRepository;
import com.sisibibi.api.global.security.AuthPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.access.AccessDeniedException;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class WebSocketAuthChannelInterceptorTest {

    private RoomParticipantRepository roomParticipantRepository;
    private WebSocketAuthChannelInterceptor interceptor;

    @BeforeEach
    void setUp() {
        roomParticipantRepository = mock(RoomParticipantRepository.class);
        interceptor = new WebSocketAuthChannelInterceptor(roomParticipantRepository);
    }

    @Test
    void preSend_setsUser_whenConnectHasHandshakePrincipal() {
        Message<byte[]> message = message(
                StompCommand.CONNECT,
                null,
                null,
                sessionAttributes(new AuthPrincipal(1L, "user@example.com", "USER"))
        );

        Message<?> result = interceptor.preSend(message, null);
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(result);

        assertThat(accessor.getUser()).isNotNull();
        assertThat(accessor.getUser().getName()).contains("user@example.com");
    }

    @Test
    void preSend_rejectsSend_whenUserIsMissing() {
        Message<byte[]> message = message(StompCommand.SEND, "/app/rooms/1/chat/messages", null, null);

        assertThatThrownBy(() -> interceptor.preSend(message, null))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void preSend_allowsSendWithoutParticipantLookup_whenUserExists() {
        AuthPrincipal principal = new AuthPrincipal(2L, "user@example.com", "USER");
        Principal user = authenticatedPrincipal(principal);
        Message<byte[]> message = message(StompCommand.SEND, "/app/rooms/1/chat/messages", user, null);

        Message<?> result = interceptor.preSend(message, null);

        assertThat(result).isNotNull();
        verify(roomParticipantRepository, never()).existsByRoomIdAndUserIdAndStatus(
                1L,
                2L,
                RoomParticipantStatus.JOINED
        );
    }

    @Test
    void preSend_allowsSubscribe_whenUserJoinedRoom() {
        AuthPrincipal principal = new AuthPrincipal(2L, "user@example.com", "USER");
        Principal user = authenticatedPrincipal(principal);
        given(roomParticipantRepository.existsByRoomIdAndUserIdAndStatus(
                1L,
                2L,
                RoomParticipantStatus.JOINED
        )).willReturn(true);
        Message<byte[]> message = message(StompCommand.SUBSCRIBE, "/topic/rooms/1/chat/messages", user, null);

        Message<?> result = interceptor.preSend(message, null);

        assertThat(result).isNotNull();
    }

    @Test
    void preSend_allowsSubscribeToWhitelistedRoomTopics_whenUserJoinedRoom() {
        AuthPrincipal principal = new AuthPrincipal(2L, "user@example.com", "USER");
        Principal user = authenticatedPrincipal(principal);
        given(roomParticipantRepository.existsByRoomIdAndUserIdAndStatus(
                1L,
                2L,
                RoomParticipantStatus.JOINED
        )).willReturn(true);

        assertThat(interceptor.preSend(
                message(StompCommand.SUBSCRIBE, "/topic/rooms/1/stage/events", user, null),
                null
        )).isNotNull();
        assertThat(interceptor.preSend(
                message(StompCommand.SUBSCRIBE, "/topic/rooms/1/participants/events", user, null),
                null
        )).isNotNull();
        assertThat(interceptor.preSend(
                message(StompCommand.SUBSCRIBE, "/topic/rooms/1/room/events", user, null),
                null
        )).isNotNull();
    }

    @Test
    void preSend_rejectsSubscribe_whenUserHasNotJoinedRoom() {
        AuthPrincipal principal = new AuthPrincipal(2L, "user@example.com", "USER");
        Principal user = authenticatedPrincipal(principal);
        given(roomParticipantRepository.existsByRoomIdAndUserIdAndStatus(
                1L,
                2L,
                RoomParticipantStatus.JOINED
        )).willReturn(false);
        Message<byte[]> message = message(StompCommand.SUBSCRIBE, "/topic/rooms/1/chat/messages", user, null);

        assertThatThrownBy(() -> interceptor.preSend(message, null))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void preSend_rejectsSubscribeToUnknownRoomTopic() {
        AuthPrincipal principal = new AuthPrincipal(2L, "user@example.com", "USER");
        Principal user = authenticatedPrincipal(principal);
        Message<byte[]> message = message(StompCommand.SUBSCRIBE, "/topic/rooms/1/events", user, null);

        assertThatThrownBy(() -> interceptor.preSend(message, null))
                .isInstanceOf(AccessDeniedException.class);
        verify(roomParticipantRepository, never()).existsByRoomIdAndUserIdAndStatus(
                1L,
                2L,
                RoomParticipantStatus.JOINED
        );
    }

    private Message<byte[]> message(
            StompCommand command,
            String destination,
            Principal user,
            Map<String, Object> sessionAttributes
    ) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
        accessor.setDestination(destination);
        accessor.setUser(user);
        if (sessionAttributes != null) {
            accessor.setSessionAttributes(sessionAttributes);
        }

        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    private Principal authenticatedPrincipal(AuthPrincipal principal) {
        return new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                principal,
                null
        );
    }

    private Map<String, Object> sessionAttributes(AuthPrincipal principal) {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put(WebSocketAuthAttributes.AUTH_PRINCIPAL, principal);
        return attributes;
    }
}
