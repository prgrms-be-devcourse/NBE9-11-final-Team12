package com.sisibibi.api.domain.chatreport.service;

import com.sisibibi.api.domain.chat.entity.ChatMessage;
import com.sisibibi.api.domain.chat.repository.ChatMessageRepository;
import com.sisibibi.api.domain.chatreport.dto.command.ChatReportCreateCommand;
import com.sisibibi.api.domain.chatreport.dto.request.ChatReportCreateReq;
import com.sisibibi.api.domain.chatreport.dto.response.ChatReportCreateRes;
import com.sisibibi.api.domain.chatreport.entity.ChatReport;
import com.sisibibi.api.domain.chatreport.entity.ChatReportReason;
import com.sisibibi.api.domain.chatreport.entity.ChatReportReviewAction;
import com.sisibibi.api.domain.chatreport.entity.ChatReportSeverity;
import com.sisibibi.api.domain.chatreport.entity.ChatReportStatus;
import com.sisibibi.api.domain.chatreport.repository.ChatReportRepository;
import com.sisibibi.api.domain.roomparticipant.entity.RoomParticipantStatus;
import com.sisibibi.api.domain.roomparticipant.repository.RoomParticipantRepository;
import com.sisibibi.api.domain.user.service.UserNicknameProvider;
import com.sisibibi.api.global.exception.CustomException;
import com.sisibibi.api.global.exception.ErrorCode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
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
class ChatReportServiceTest {

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @Mock
    private ChatReportRepository chatReportRepository;

    @Mock
    private RoomParticipantRepository roomParticipantRepository;

    @Mock
    private UserNicknameProvider userNicknameProvider;

    @InjectMocks
    private ChatReportService chatReportService;

    @Test
    void getReports_returnsSummaryPage() {
        ChatReport report = report(100L, ChatReportStatus.PENDING);
        PageRequest pageable = PageRequest.of(0, 20);
        given(chatReportRepository.findAllByFilters(
                ChatReportStatus.PENDING,
                ChatReportReason.SPAM,
                pageable
        )).willReturn(new PageImpl<>(List.of(report), pageable, 1));
        given(userNicknameProvider.findNicknamesByIds(any()))
                .willReturn(Map.of(20L, "신고자", 30L, "대상자"));

        var response = chatReportService.getReports(
                ChatReportStatus.PENDING,
                ChatReportReason.SPAM,
                pageable
        );

        assertThat(response.getTotalElements()).isEqualTo(1);
        assertThat(response.getContent().get(0).reportId()).isEqualTo(100L);
        assertThat(response.getContent().get(0).reportedUserNickname()).isEqualTo("대상자");
        assertThat(response.getContent().get(0).reporterUserNickname()).isEqualTo("신고자");
        assertThat(response.getContent().get(0).status()).isEqualTo(ChatReportStatus.PENDING);
    }

    @Test
    void reviewReport_startsReview() {
        ChatReport report = report(100L, ChatReportStatus.PENDING);
        given(chatReportRepository.findByIdForUpdate(100L)).willReturn(Optional.of(report));
        given(userNicknameProvider.findNicknamesByIds(any()))
                .willReturn(Map.of(1L, "관리자"));

        var response = chatReportService.reviewReport(
                100L,
                1L,
                ChatReportReviewAction.START_REVIEW,
                null,
                null
        );

        assertThat(response.reportId()).isEqualTo(100L);
        assertThat(response.status()).isEqualTo(ChatReportStatus.REVIEWING);
        assertThat(response.reviewedBy()).isEqualTo(1L);
        assertThat(response.reviewedByNickname()).isEqualTo("관리자");
    }

    @Test
    void reviewReport_throwsSeverityRequired_whenResolveWithoutSeverity() {
        ChatReport report = report(100L, ChatReportStatus.REVIEWING);
        given(chatReportRepository.findByIdForUpdate(100L)).willReturn(Optional.of(report));

        assertThatThrownBy(() -> chatReportService.reviewReport(
                100L,
                1L,
                ChatReportReviewAction.RESOLVE,
                "위반 확인",
                null
        ))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.CHAT_REPORT_SEVERITY_REQUIRED);
    }

    @Test
    void reviewReport_resolvesReviewingReport() {
        ChatReport report = report(100L, ChatReportStatus.REVIEWING);
        given(chatReportRepository.findByIdForUpdate(100L)).willReturn(Optional.of(report));
        given(userNicknameProvider.findNicknamesByIds(any()))
                .willReturn(Map.of(1L, "관리자"));

        var response = chatReportService.reviewReport(
                100L,
                1L,
                ChatReportReviewAction.RESOLVE,
                "채팅 운영 정책 위반",
                ChatReportSeverity.MEDIUM
        );

        assertThat(response.status()).isEqualTo(ChatReportStatus.RESOLVED);
        assertThat(response.severity()).isEqualTo(ChatReportSeverity.MEDIUM);
        assertThat(response.resolutionNote()).isEqualTo("채팅 운영 정책 위반");
    }

    @Test
    void createReport_savesPendingReport() {
        ChatMessage message = message(10L, 1L, 30L, "other", "신고 대상 채팅");
        givenParticipating(1L, 20L, true);
        given(chatMessageRepository.findByIdAndRoomIdAndDeletedFalse(10L, 1L))
                .willReturn(Optional.of(message));
        given(chatReportRepository.existsByMessageIdAndReporterUserId(10L, 20L))
                .willReturn(false);
        given(chatReportRepository.save(any(ChatReport.class))).willAnswer(invocation -> {
            ChatReport report = invocation.getArgument(0);
            ReflectionTestUtils.setField(report, "id", 100L);
            ReflectionTestUtils.setField(report, "createdAt", LocalDateTime.of(2026, 6, 26, 10, 0));
            return report;
        });

        ChatReportCreateRes response = chatReportService.createReport(
                1L,
                10L,
                20L,
                new ChatReportCreateReq(ChatReportReason.HATE_SPEECH, "혐오 표현입니다.")
        );

        ArgumentCaptor<ChatReport> captor = ArgumentCaptor.forClass(ChatReport.class);
        verify(chatReportRepository).save(captor.capture());
        ChatReport saved = captor.getValue();
        assertThat(saved.getRoomId()).isEqualTo(1L);
        assertThat(saved.getMessageId()).isEqualTo(10L);
        assertThat(saved.getReportedUserId()).isEqualTo(30L);
        assertThat(saved.getReporterUserId()).isEqualTo(20L);
        assertThat(saved.getContentSnapshot()).isEqualTo("신고 대상 채팅");
        assertThat(saved.getStatus()).isEqualTo(ChatReportStatus.PENDING);
        assertThat(response.reportId()).isEqualTo(100L);
        assertThat(response.status()).isEqualTo(ChatReportStatus.PENDING);
    }

    @Test
    void createReport_throwsDescriptionRequired_whenReasonIsOtherAndDescriptionIsBlank() {
        assertThatThrownBy(() -> chatReportService.createReport(
                1L,
                10L,
                20L,
                new ChatReportCreateReq(ChatReportReason.OTHER, " ")
        ))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.CHAT_REPORT_DESCRIPTION_REQUIRED);

        verify(chatMessageRepository, never()).findByIdAndRoomIdAndDeletedFalse(10L, 1L);
    }

    @Test
    void createReport_throwsParticipationRequired_whenReporterIsNotParticipant() {
        givenParticipating(1L, 20L, false);

        assertThatThrownBy(() -> chatReportService.createReport(
                1L,
                10L,
                20L,
                new ChatReportCreateReq(ChatReportReason.SPAM, null)
        ))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ROOM_PARTICIPATION_REQUIRED);
    }

    @Test
    void createReport_throwsMessageNotFound_whenMessageDoesNotExistOrIsDeleted() {
        givenParticipating(1L, 20L, true);
        given(chatMessageRepository.findByIdAndRoomIdAndDeletedFalse(10L, 1L))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> chatReportService.createReport(
                1L,
                10L,
                20L,
                new ChatReportCreateReq(ChatReportReason.SPAM, null)
        ))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.CHAT_MESSAGE_NOT_FOUND);
    }

    @Test
    void createReport_throwsSelfReportNotAllowed_whenReporterOwnsMessage() {
        ChatMessage message = message(10L, 1L, 20L, "me", "내 채팅");
        givenParticipating(1L, 20L, true);
        given(chatMessageRepository.findByIdAndRoomIdAndDeletedFalse(10L, 1L))
                .willReturn(Optional.of(message));

        assertThatThrownBy(() -> chatReportService.createReport(
                1L,
                10L,
                20L,
                new ChatReportCreateReq(ChatReportReason.SPAM, null)
        ))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.CHAT_REPORT_SELF_NOT_ALLOWED);

        verify(chatReportRepository, never()).save(any(ChatReport.class));
    }

    @Test
    void createReport_throwsAlreadyExists_whenReporterAlreadyReportedMessage() {
        ChatMessage message = message(10L, 1L, 30L, "other", "중복 신고 대상");
        givenParticipating(1L, 20L, true);
        given(chatMessageRepository.findByIdAndRoomIdAndDeletedFalse(10L, 1L))
                .willReturn(Optional.of(message));
        given(chatReportRepository.existsByMessageIdAndReporterUserId(10L, 20L))
                .willReturn(true);

        assertThatThrownBy(() -> chatReportService.createReport(
                1L,
                10L,
                20L,
                new ChatReportCreateReq(ChatReportReason.SPAM, null)
        ))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.CHAT_REPORT_ALREADY_EXISTS);

        verify(chatReportRepository, never()).save(any(ChatReport.class));
    }

    private void givenParticipating(Long roomId, Long userId, boolean participating) {
        given(roomParticipantRepository.existsByRoomIdAndUserIdAndStatus(
                roomId,
                userId,
                RoomParticipantStatus.JOINED
        )).willReturn(participating);
    }

    private ChatMessage message(Long id, Long roomId, Long userId, String nickname, String content) {
        ChatMessage message = ChatMessage.create(roomId, userId, nickname, content);
        ReflectionTestUtils.setField(message, "id", id);
        ReflectionTestUtils.setField(message, "createdAt", LocalDateTime.of(2026, 6, 26, 9, 0));
        return message;
    }

    private ChatReport report(Long id, ChatReportStatus status) {
        ChatReport report = ChatReport.create(new ChatReportCreateCommand(
                1L,
                10L,
                30L,
                20L,
                "신고 대상 채팅",
                ChatReportReason.SPAM,
                null
        ));
        ReflectionTestUtils.setField(report, "id", id);
        ReflectionTestUtils.setField(report, "status", status);
        ReflectionTestUtils.setField(report, "createdAt", LocalDateTime.of(2026, 6, 26, 10, 0));
        ReflectionTestUtils.setField(report, "updatedAt", LocalDateTime.of(2026, 6, 26, 10, 0));
        return report;
    }
}
