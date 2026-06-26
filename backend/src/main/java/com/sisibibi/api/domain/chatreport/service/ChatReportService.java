package com.sisibibi.api.domain.chatreport.service;

import com.sisibibi.api.domain.chat.entity.ChatMessage;
import com.sisibibi.api.domain.chat.repository.ChatMessageRepository;
import com.sisibibi.api.domain.chatreport.dto.request.ChatReportCreateReq;
import com.sisibibi.api.domain.chatreport.dto.response.ChatReportCreateRes;
import com.sisibibi.api.domain.chatreport.entity.ChatReport;
import com.sisibibi.api.domain.chatreport.entity.ChatReportReason;
import com.sisibibi.api.domain.chatreport.repository.ChatReportRepository;
import com.sisibibi.api.domain.roomparticipant.entity.RoomParticipantStatus;
import com.sisibibi.api.domain.roomparticipant.repository.RoomParticipantRepository;
import com.sisibibi.api.global.exception.CustomException;
import com.sisibibi.api.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatReportService {

    private final ChatMessageRepository chatMessageRepository;
    private final ChatReportRepository chatReportRepository;
    private final RoomParticipantRepository roomParticipantRepository;

    @Transactional
    public ChatReportCreateRes createReport(
            Long roomId,
            Long messageId,
            Long reporterUserId,
            ChatReportCreateReq request
    ) {
        if (request.reason() == ChatReportReason.OTHER
                && (request.description() == null || request.description().isBlank())) {
            throw new CustomException(ErrorCode.CHAT_REPORT_DESCRIPTION_REQUIRED);
        }

        validateParticipation(roomId, reporterUserId);

        ChatMessage message = chatMessageRepository.findByIdAndRoomIdAndDeletedFalse(messageId, roomId)
                .orElseThrow(() -> new CustomException(ErrorCode.CHAT_MESSAGE_NOT_FOUND));

        if (message.getUserId().equals(reporterUserId)) {
            log.warn("Self chat report blocked. roomId={}, messageId={}, reporterUserId={}",
                    roomId,
                    messageId,
                    reporterUserId
            );
            throw new CustomException(ErrorCode.CHAT_REPORT_SELF_NOT_ALLOWED);
        }

        if (chatReportRepository.existsByMessageIdAndReporterUserId(messageId, reporterUserId)) {
            log.warn("Duplicate chat report blocked. roomId={}, messageId={}, reporterUserId={}",
                    roomId,
                    messageId,
                    reporterUserId
            );
            throw new CustomException(ErrorCode.CHAT_REPORT_ALREADY_EXISTS);
        }

        ChatReport report = ChatReport.create(
                roomId,
                messageId,
                message.getUserId(),
                reporterUserId,
                message.getContent(),
                request.reason(),
                request.description()
        );
        ChatReport savedReport = chatReportRepository.save(report);

        log.info(
                "Chat report created. reportId={}, roomId={}, messageId={}, reporterUserId={}, reportedUserId={}, reason={}",
                savedReport.getId(),
                roomId,
                messageId,
                reporterUserId,
                message.getUserId(),
                request.reason()
        );

        return ChatReportCreateRes.from(savedReport);
    }

    private void validateParticipation(Long roomId, Long userId) {
        boolean participating = roomParticipantRepository.existsByRoomIdAndUserIdAndStatus(
                roomId,
                userId,
                RoomParticipantStatus.JOINED
        );
        if (!participating) {
            throw new CustomException(ErrorCode.ROOM_PARTICIPATION_REQUIRED);
        }
    }
}
