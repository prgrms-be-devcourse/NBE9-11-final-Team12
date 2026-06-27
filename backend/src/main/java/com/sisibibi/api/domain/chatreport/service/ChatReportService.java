package com.sisibibi.api.domain.chatreport.service;

import com.sisibibi.api.domain.chat.entity.ChatMessage;
import com.sisibibi.api.domain.chat.repository.ChatMessageRepository;
import com.sisibibi.api.domain.chatreport.dto.command.ChatReportCreateCommand;
import com.sisibibi.api.domain.chatreport.dto.request.ChatReportCreateReq;
import com.sisibibi.api.domain.chatreport.dto.response.ChatReportCreateRes;
import com.sisibibi.api.domain.chatreport.dto.response.ChatReportDetailRes;
import com.sisibibi.api.domain.chatreport.dto.response.ChatReportReviewRes;
import com.sisibibi.api.domain.chatreport.dto.response.ChatReportSummaryRes;
import com.sisibibi.api.domain.chatreport.entity.ChatReport;
import com.sisibibi.api.domain.chatreport.entity.ChatReportReason;
import com.sisibibi.api.domain.chatreport.entity.ChatReportReviewAction;
import com.sisibibi.api.domain.chatreport.entity.ChatReportSeverity;
import com.sisibibi.api.domain.chatreport.entity.ChatReportStatus;
import com.sisibibi.api.domain.chatreport.repository.ChatReportRepository;
import com.sisibibi.api.domain.roomparticipant.entity.RoomParticipantStatus;
import com.sisibibi.api.domain.roomparticipant.repository.RoomParticipantRepository;
import com.sisibibi.api.domain.user.entity.User;
import com.sisibibi.api.domain.user.repository.UserRepository;
import com.sisibibi.api.global.exception.CustomException;
import com.sisibibi.api.global.exception.ErrorCode;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatReportService {

    private final ChatMessageRepository chatMessageRepository;
    private final ChatReportRepository chatReportRepository;
    private final RoomParticipantRepository roomParticipantRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public Page<ChatReportSummaryRes> getReports(
            ChatReportStatus status,
            ChatReportReason reason,
            Pageable pageable
    ) {
        Page<ChatReport> reports = chatReportRepository.findAllByFilters(status, reason, pageable);
        Map<Long, String> nicknames = findNicknames(extractUserIds(reports.getContent()));

        return reports.map(report -> ChatReportSummaryRes.from(
                report,
                nicknames.get(report.getReportedUserId()),
                nicknames.get(report.getReporterUserId())
        ));
    }

    @Transactional(readOnly = true)
    public ChatReportDetailRes getReport(Long reportId) {
        ChatReport report = chatReportRepository.findById(reportId)
                .orElseThrow(() -> new CustomException(ErrorCode.CHAT_REPORT_NOT_FOUND));

        Map<Long, String> nicknames = findNicknames(extractUserIds(Set.of(report)));

        return ChatReportDetailRes.from(
                report,
                nicknames.get(report.getReportedUserId()),
                nicknames.get(report.getReporterUserId()),
                nicknames.get(report.getReviewedBy())
        );
    }

    @Transactional
    public ChatReportReviewRes reviewReport(
            Long reportId,
            Long reviewerUserId,
            ChatReportReviewAction action,
            String resolutionNote,
            ChatReportSeverity severity
    ) {
        ChatReport report = chatReportRepository.findByIdForUpdate(reportId)
                .orElseThrow(() -> new CustomException(ErrorCode.CHAT_REPORT_NOT_FOUND));

        report.review(action, reviewerUserId, resolutionNote, severity, LocalDateTime.now());
        log.info(
                "Chat report reviewed. reportId={}, reviewerUserId={}, action={}, status={}, severity={}",
                reportId,
                reviewerUserId,
                action,
                report.getStatus(),
                report.getSeverity()
        );

        Map<Long, String> nicknames = findNicknames(Set.of(reviewerUserId));
        return ChatReportReviewRes.from(report, nicknames.get(reviewerUserId));
    }

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

        ChatReport report = ChatReport.create(new ChatReportCreateCommand(
                roomId,
                messageId,
                message.getUserId(),
                reporterUserId,
                message.getContent(),
                request.reason(),
                request.description()
        ));
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

    private Set<Long> extractUserIds(Collection<ChatReport> reports) {
        return reports.stream()
                .flatMap(report -> Stream.of(
                        report.getReportedUserId(),
                        report.getReporterUserId(),
                        report.getReviewedBy()
                ))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private Map<Long, String> findNicknames(Set<Long> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }

        return userRepository.findAllById(userIds)
                .stream()
                .collect(Collectors.toMap(User::getId, User::getNickname, (left, right) -> left));
    }
}
