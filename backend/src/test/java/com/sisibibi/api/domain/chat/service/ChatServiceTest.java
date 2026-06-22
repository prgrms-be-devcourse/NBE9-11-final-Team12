package com.sisibibi.api.domain.chat.service;

import com.sisibibi.api.domain.chat.dto.event.ChatEventType;
import com.sisibibi.api.domain.chat.dto.event.ChatMessageChangedEvent;
import com.sisibibi.api.domain.chat.dto.response.ChatMessageCursorPageRes;
import com.sisibibi.api.domain.chat.entity.ChatMessage;
import com.sisibibi.api.domain.chat.repository.ChatMessageRepository;
import com.sisibibi.api.domain.room.entity.Room;
import com.sisibibi.api.domain.room.entity.RoomStatus;
import com.sisibibi.api.domain.room.repository.RoomRepository;
import com.sisibibi.api.domain.roomparticipant.entity.RoomParticipantStatus;
import com.sisibibi.api.domain.roomparticipant.repository.RoomParticipantRepository;
import com.sisibibi.api.domain.user.entity.User;
import com.sisibibi.api.domain.user.entity.UserStatus;
import com.sisibibi.api.domain.user.repository.UserRepository;
import com.sisibibi.api.domain.usersanction.service.UserSanctionPolicyService;
import com.sisibibi.api.global.exception.CustomException;
import com.sisibibi.api.global.exception.ErrorCode;
import com.sisibibi.api.global.moderation.ProfanityDetector;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private RoomParticipantRepository roomParticipantRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserSanctionPolicyService userSanctionPolicyService;

    @Mock
    private ProfanityDetector profanityDetector;

    @Mock
    private ChatRateLimiter chatRateLimiter;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private ChatService chatService;

    @Test
    void createMessage_throwsChatRestricted_whenUserHasActiveSanction() {
        Long roomId = 1L;
        Long userId = 2L;
        User user = user(UserStatus.ACTIVE);
        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        doThrow(new CustomException(ErrorCode.USER_CHAT_RESTRICTED))
                .when(userSanctionPolicyService)
                .validateChatAllowed(userId);

        assertThatThrownBy(() -> chatService.createMessage(roomId, userId, "hello"))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USER_CHAT_RESTRICTED);

        verify(chatMessageRepository, never()).save(any(ChatMessage.class));
    }

    @Test
    void createMessage_savesMessageAndSchedulesPublish_whenRequestIsValid() {
        Long roomId = 1L;
        Long userId = 2L;
        User user = user(UserStatus.ACTIVE);
        given(user.getNickname()).willReturn("tester");
        Room room = room(RoomStatus.OPEN);
        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(roomRepository.findById(roomId)).willReturn(Optional.of(room));
        given(roomParticipantRepository.existsByRoomIdAndUserIdAndStatus(
                roomId,
                userId,
                RoomParticipantStatus.JOINED
        )).willReturn(true);
        given(profanityDetector.containsProfanity("hello")).willReturn(false);
        given(chatMessageRepository.save(any(ChatMessage.class))).willAnswer(invocation -> {
            ChatMessage message = invocation.getArgument(0);
            ReflectionTestUtils.setField(message, "id", 10L);
            ReflectionTestUtils.setField(message, "createdAt", LocalDateTime.of(2026, 6, 15, 10, 0));
            return message;
        });

        chatService.createMessage(roomId, userId, "hello");

        ArgumentCaptor<ChatMessage> messageCaptor = ArgumentCaptor.forClass(ChatMessage.class);
        verify(chatMessageRepository).save(messageCaptor.capture());
        ChatMessage saved = messageCaptor.getValue();
        assertThat(saved.getRoomId()).isEqualTo(roomId);
        assertThat(saved.getUserId()).isEqualTo(userId);
        assertThat(saved.getNicknameSnapshot()).isEqualTo("tester");
        assertThat(saved.getContent()).isEqualTo("hello");
        assertThat(saved.isDeleted()).isFalse();
        ArgumentCaptor<ChatMessageChangedEvent> eventCaptor =
                ArgumentCaptor.forClass(ChatMessageChangedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        ChatMessageChangedEvent event = eventCaptor.getValue();
        assertThat(event.type()).isEqualTo(ChatEventType.MESSAGE_CREATED);
        assertThat(event.roomId()).isEqualTo(roomId);
        assertThat(event.payload().type()).isEqualTo(ChatEventType.MESSAGE_CREATED);
        assertThat(event.payload().messageId()).isEqualTo(10L);
        assertThat(event.payload().roomId()).isEqualTo(roomId);
        assertThat(event.payload().userId()).isEqualTo(userId);
        assertThat(event.payload().nicknameSnapshot()).isEqualTo("tester");
        assertThat(event.payload().content()).isEqualTo("hello");
    }

    @Test
    void createMessage_throwsRoomNotFound_whenRoomDoesNotExist() {
        User user = user(UserStatus.ACTIVE);
        given(userRepository.findById(2L)).willReturn(Optional.of(user));
        given(roomRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> chatService.createMessage(1L, 2L, "hello"))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ROOM_NOT_FOUND);
    }

    @Test
    void createMessage_throwsRoomClosed_whenRoomIsClosed() {
        User user = user(UserStatus.ACTIVE);
        Room room = room(RoomStatus.CLOSED);
        given(userRepository.findById(2L)).willReturn(Optional.of(user));
        given(roomRepository.findById(1L)).willReturn(Optional.of(room));

        assertThatThrownBy(() -> chatService.createMessage(1L, 2L, "hello"))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ROOM_CLOSED);
    }

    @Test
    void createMessage_throwsParticipationRequired_whenUserHasNotJoinedRoom() {
        User user = user(UserStatus.ACTIVE);
        Room room = room(RoomStatus.OPEN);
        given(userRepository.findById(2L)).willReturn(Optional.of(user));
        given(roomRepository.findById(1L)).willReturn(Optional.of(room));
        given(roomParticipantRepository.existsByRoomIdAndUserIdAndStatus(
                1L,
                2L,
                RoomParticipantStatus.JOINED
        )).willReturn(false);

        assertThatThrownBy(() -> chatService.createMessage(1L, 2L, "hello"))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ROOM_PARTICIPATION_REQUIRED);
    }

    @Test
    void createMessage_throwsUserBanned_whenUserIsBanned() {
        User user = user(UserStatus.BANNED);
        given(userRepository.findById(2L)).willReturn(Optional.of(user));

        assertThatThrownBy(() -> chatService.createMessage(1L, 2L, "hello"))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USER_BANNED);
    }

    @Test
    void createMessage_throwsEmpty_whenContentIsBlank() {
        assertThatThrownBy(() -> chatService.createMessage(1L, 2L, "   "))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.CHAT_MESSAGE_EMPTY);

        verify(chatRateLimiter, never()).check(2L);
        verify(chatMessageRepository, never()).save(any(ChatMessage.class));
    }

    @Test
    void createMessage_throwsTooLong_whenContentExceedsLimit() {
        String content = "a".repeat(301);

        assertThatThrownBy(() -> chatService.createMessage(1L, 2L, content))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.CHAT_MESSAGE_TOO_LONG);
    }

    @Test
    void createMessage_throwsProfanity_whenContentContainsProfanity() {
        User user = user(UserStatus.ACTIVE);
        Room room = room(RoomStatus.OPEN);
        given(userRepository.findById(2L)).willReturn(Optional.of(user));
        given(roomRepository.findById(1L)).willReturn(Optional.of(room));
        given(roomParticipantRepository.existsByRoomIdAndUserIdAndStatus(
                1L,
                2L,
                RoomParticipantStatus.JOINED
        )).willReturn(true);
        given(profanityDetector.containsProfanity("bad content")).willReturn(true);

        assertThatThrownBy(() -> chatService.createMessage(1L, 2L, "bad content"))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.CHAT_MESSAGE_CONTAINS_PROFANITY);

        verify(chatMessageRepository, never()).save(any(ChatMessage.class));
    }

    @Test
    void createMessage_stops_whenRateLimitExceeded() {
        org.mockito.BDDMockito.willThrow(new CustomException(ErrorCode.CHAT_RATE_LIMIT_EXCEEDED))
                .given(chatRateLimiter)
                .check(2L);

        assertThatThrownBy(() -> chatService.createMessage(1L, 2L, "hello"))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.CHAT_RATE_LIMIT_EXCEEDED);

        verify(userRepository, never()).findById(2L);
    }

    @Test
    void getMessages_returnsCursorPageAndExcludesExtraItem() {
        ChatMessage first = message(10L, 1L, 2L, "tester", "latest");
        ChatMessage second = message(9L, 1L, 3L, "other", "previous");
        ChatMessage extra = message(8L, 1L, 4L, "next", "extra");
        given(roomRepository.existsById(1L)).willReturn(true);
        given(roomParticipantRepository.existsByRoomIdAndUserIdAndStatus(
                1L,
                2L,
                RoomParticipantStatus.JOINED
        )).willReturn(true);
        given(chatMessageRepository.findVisibleByRoomIdBeforeCursor(
                org.mockito.ArgumentMatchers.eq(1L),
                org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.any()
        )).willReturn(List.of(first, second, extra));

        ChatMessageCursorPageRes response = chatService.getMessages(1L, 2L, null, 2);

        assertThat(response.items()).extracting("messageId").containsExactly(10L, 9L);
        assertThat(response.nextCursor()).isEqualTo(9L);
        assertThat(response.hasNext()).isTrue();
    }

    @Test
    void getMessages_throwsRoomNotFound_whenRoomDoesNotExist() {
        given(roomRepository.existsById(1L)).willReturn(false);

        assertThatThrownBy(() -> chatService.getMessages(1L, 2L, null, 50))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ROOM_NOT_FOUND);
    }

    @Test
    void getMessages_throwsParticipationRequired_whenUserHasNotJoinedRoom() {
        given(roomRepository.existsById(1L)).willReturn(true);
        given(roomParticipantRepository.existsByRoomIdAndUserIdAndStatus(
                1L,
                2L,
                RoomParticipantStatus.JOINED
        )).willReturn(false);

        assertThatThrownBy(() -> chatService.getMessages(1L, 2L, null, 50))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ROOM_PARTICIPATION_REQUIRED);
    }

    @Test
    void deleteMessage_softDeletesOwnMessageAndSchedulesDeletedEvent() {
        ChatMessage message = message(10L, 1L, 2L, "tester", "hello");
        given(roomParticipantRepository.existsByRoomIdAndUserIdAndStatus(
                1L,
                2L,
                RoomParticipantStatus.JOINED
        )).willReturn(true);
        given(chatMessageRepository.findByIdAndRoomIdAndDeletedFalse(10L, 1L))
                .willReturn(Optional.of(message));

        chatService.deleteMessage(1L, 10L, 2L);

        assertThat(message.isDeleted()).isTrue();
        assertThat(message.getDeletedBy()).isEqualTo(2L);
        assertThat(message.getDeletedAt()).isNotNull();
        ArgumentCaptor<ChatMessageChangedEvent> eventCaptor =
                ArgumentCaptor.forClass(ChatMessageChangedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().type()).isEqualTo(ChatEventType.MESSAGE_DELETED);
        assertThat(eventCaptor.getValue().roomId()).isEqualTo(1L);
        assertThat(eventCaptor.getValue().payload().content()).isNull();
    }

    @Test
    void deleteMessage_throwsForbidden_whenUserIsNotAuthor() {
        ChatMessage message = message(10L, 1L, 9L, "other", "hello");
        given(roomParticipantRepository.existsByRoomIdAndUserIdAndStatus(
                1L,
                2L,
                RoomParticipantStatus.JOINED
        )).willReturn(true);
        given(chatMessageRepository.findByIdAndRoomIdAndDeletedFalse(10L, 1L))
                .willReturn(Optional.of(message));

        assertThatThrownBy(() -> chatService.deleteMessage(1L, 10L, 2L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.FORBIDDEN);
    }

    @Test
    void deleteMessage_throwsMessageNotFound_whenMessageDoesNotExist() {
        given(roomParticipantRepository.existsByRoomIdAndUserIdAndStatus(
                1L,
                2L,
                RoomParticipantStatus.JOINED
        )).willReturn(true);
        given(chatMessageRepository.findByIdAndRoomIdAndDeletedFalse(10L, 1L))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> chatService.deleteMessage(1L, 10L, 2L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.CHAT_MESSAGE_NOT_FOUND);
    }

    private User user(UserStatus status) {
        User user = org.mockito.Mockito.mock(User.class);
        given(user.getStatus()).willReturn(status);
        return user;
    }

    private Room room(RoomStatus status) {
        Room room = org.mockito.Mockito.mock(Room.class);
        given(room.getStatus()).willReturn(status);
        return room;
    }

    private ChatMessage message(Long id, Long roomId, Long userId, String nickname, String content) {
        ChatMessage message = ChatMessage.create(roomId, userId, nickname, content);
        ReflectionTestUtils.setField(message, "id", id);
        ReflectionTestUtils.setField(message, "createdAt", LocalDateTime.of(2026, 6, 16, 10, 0));
        return message;
    }
}
