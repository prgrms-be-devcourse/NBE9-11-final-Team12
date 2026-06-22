package com.sisibibi.api.domain.chat.service;

import com.sisibibi.api.domain.chat.dto.event.ChatMessageChangedEvent;
import com.sisibibi.api.domain.chat.dto.event.ChatMessageEventPayload;
import com.sisibibi.api.domain.chat.dto.response.ChatMessageCursorPageRes;
import com.sisibibi.api.domain.chat.dto.response.ChatMessageRes;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private static final int MAX_CONTENT_LENGTH = 300;

    private final ChatMessageRepository chatMessageRepository;
    private final RoomRepository roomRepository;
    private final RoomParticipantRepository roomParticipantRepository;
    private final UserRepository userRepository;
    private final UserSanctionPolicyService userSanctionPolicyService;
    private final ProfanityDetector profanityDetector;
    private final ChatRateLimiter chatRateLimiter;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void createMessage(Long roomId, Long userId, String content) {
        validateContent(content);
        chatRateLimiter.check(userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        if (user.getStatus() == UserStatus.BANNED) {
            log.warn("Chat message blocked for banned user. roomId={}, userId={}", roomId, userId);
            throw new CustomException(ErrorCode.USER_BANNED);
        }
        userSanctionPolicyService.validateChatAllowed(userId);

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new CustomException(ErrorCode.ROOM_NOT_FOUND));
        if (room.getStatus() != RoomStatus.OPEN) {
            throw new CustomException(ErrorCode.ROOM_CLOSED);
        }

        validateParticipation(roomId, userId);

        if (profanityDetector.containsProfanity(content)) {
            log.warn("Chat message blocked by profanity detector. roomId={}, userId={}", roomId, userId);
            throw new CustomException(ErrorCode.CHAT_MESSAGE_CONTAINS_PROFANITY);
        }

        ChatMessage saved = chatMessageRepository.save(ChatMessage.create(
                roomId,
                userId,
                user.getNickname(),
                content
        ));
        ChatMessageEventPayload event = ChatMessageEventPayload.created(saved);
        publishChatMessageChangedEvent(event);
    }

    @Transactional(readOnly = true)
    public ChatMessageCursorPageRes getMessages(Long roomId, Long userId, Long cursor, int limit) {
        if (!roomRepository.existsById(roomId)) {
            throw new CustomException(ErrorCode.ROOM_NOT_FOUND);
        }

        validateParticipation(roomId, userId);

        List<ChatMessage> messages = chatMessageRepository.findVisibleByRoomIdBeforeCursor(
                roomId,
                cursor,
                PageRequest.of(0, limit + 1)
        );
        boolean hasNext = messages.size() > limit;
        List<ChatMessageRes> items = messages.stream()
                .limit(limit)
                .map(ChatMessageRes::from)
                .toList();
        Long nextCursor = hasNext ? items.get(items.size() - 1).messageId() : null;

        return new ChatMessageCursorPageRes(items, nextCursor, hasNext);
    }

    @Transactional
    public void deleteMessage(Long roomId, Long messageId, Long userId) {
        validateParticipation(roomId, userId);

        ChatMessage message = chatMessageRepository.findByIdAndRoomIdAndDeletedFalse(messageId, roomId)
                .orElseThrow(() -> new CustomException(ErrorCode.CHAT_MESSAGE_NOT_FOUND));

        if (!message.getUserId().equals(userId)) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }

        message.softDelete(userId, LocalDateTime.now());
        publishChatMessageChangedEvent(ChatMessageEventPayload.deleted(message));
    }

    private void publishChatMessageChangedEvent(ChatMessageEventPayload event) {
        eventPublisher.publishEvent(new ChatMessageChangedEvent(
                event.type(),
                event.roomId(),
                event
        ));
    }

    private void validateContent(String content) {
        if (!StringUtils.hasText(content)) {
            throw new CustomException(ErrorCode.CHAT_MESSAGE_EMPTY);
        }

        if (content.length() > MAX_CONTENT_LENGTH) {
            throw new CustomException(ErrorCode.CHAT_MESSAGE_TOO_LONG);
        }
    }

    private void validateParticipation(Long roomId, Long userId) {
        boolean participating = roomParticipantRepository.existsByRoomIdAndUserIdAndStatus(
                roomId,
                userId,
                RoomParticipantStatus.JOINED
        );
        if (!participating) {
            log.warn(
                    "Chat action blocked because user is not participating. roomId={}, userId={}",
                    roomId,
                    userId
            );
            throw new CustomException(ErrorCode.ROOM_PARTICIPATION_REQUIRED);
        }
    }
}
