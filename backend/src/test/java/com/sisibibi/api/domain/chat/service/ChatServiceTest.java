package com.sisibibi.api.domain.chat.service;

import com.sisibibi.api.domain.chat.dto.response.ChatEventRes;
import com.sisibibi.api.domain.chat.entity.ChatEventType;
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
import com.sisibibi.api.global.exception.CustomException;
import com.sisibibi.api.global.exception.ErrorCode;
import com.sisibibi.api.global.moderation.ProfanityDetector;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
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
    private ProfanityDetector profanityDetector;

    @Mock
    private ChatRateLimiter chatRateLimiter;

    @Mock
    private AfterCommitChatMessagePublisher afterCommitChatMessagePublisher;

    @InjectMocks
    private ChatService chatService;

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

        ChatEventRes response = chatService.createMessage(roomId, userId, "hello");

        ArgumentCaptor<ChatMessage> messageCaptor = ArgumentCaptor.forClass(ChatMessage.class);
        verify(chatMessageRepository).save(messageCaptor.capture());
        ChatMessage saved = messageCaptor.getValue();
        assertThat(saved.getRoomId()).isEqualTo(roomId);
        assertThat(saved.getUserId()).isEqualTo(userId);
        assertThat(saved.getNicknameSnapshot()).isEqualTo("tester");
        assertThat(saved.getContent()).isEqualTo("hello");
        assertThat(saved.isDeleted()).isFalse();
        assertThat(response.type()).isEqualTo(ChatEventType.MESSAGE_CREATED);
        assertThat(response.messageId()).isEqualTo(10L);
        verify(afterCommitChatMessagePublisher).publishAfterCommit(response);
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
}
