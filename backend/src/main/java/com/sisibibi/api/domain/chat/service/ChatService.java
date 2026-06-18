package com.sisibibi.api.domain.chat.service;

import com.sisibibi.api.domain.chat.dto.response.ChatEventRes;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    private final ProfanityDetector profanityDetector;
    private final ChatRateLimiter chatRateLimiter;
    private final AfterCommitChatMessagePublisher afterCommitChatMessagePublisher;

    @Transactional
    public ChatEventRes createMessage(Long roomId, Long userId, String content) {
        validateContent(content);
        chatRateLimiter.check(userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        if (user.getStatus() == UserStatus.BANNED) {
            log.warn("Chat message blocked for banned user. roomId={}, userId={}", roomId, userId);
            throw new CustomException(ErrorCode.USER_BANNED);
        }

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new CustomException(ErrorCode.ROOM_NOT_FOUND));
        if (room.getStatus() != RoomStatus.OPEN) {
            throw new CustomException(ErrorCode.ROOM_CLOSED);
        }

        boolean participating = roomParticipantRepository.existsByRoomIdAndUserIdAndStatus(
                roomId,
                userId,
                RoomParticipantStatus.JOINED
        );
        if (!participating) {
            log.warn(
                    "Chat message blocked because user is not participating. roomId={}, userId={}",
                    roomId,
                    userId
            );
            throw new CustomException(ErrorCode.ROOM_PARTICIPATION_REQUIRED);
        }

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
        ChatEventRes event = ChatEventRes.created(saved);
        afterCommitChatMessagePublisher.publishAfterCommit(event);
        return event;
    }

    private void validateContent(String content) {
        if (!StringUtils.hasText(content)) {
            throw new CustomException(ErrorCode.CHAT_MESSAGE_EMPTY);
        }

        if (content.length() > MAX_CONTENT_LENGTH) {
            throw new CustomException(ErrorCode.CHAT_MESSAGE_TOO_LONG);
        }
    }
}
