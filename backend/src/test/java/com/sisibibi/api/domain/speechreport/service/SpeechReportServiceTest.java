package com.sisibibi.api.domain.speechreport.service;

import com.sisibibi.api.domain.speech.entity.Speech;
import com.sisibibi.api.domain.speech.entity.SpeechDeleteReason;
import com.sisibibi.api.domain.speech.entity.SpeechStance;
import com.sisibibi.api.domain.speech.repository.SpeechRepository;
import com.sisibibi.api.domain.speechreport.dto.command.SpeechReportCreateCommand;
import com.sisibibi.api.domain.speechreport.dto.response.SpeechReportCreateRes;
import com.sisibibi.api.domain.speechreport.dto.response.SpeechReportDetailRes;
import com.sisibibi.api.domain.speechreport.dto.response.SpeechReportSummaryRes;
import com.sisibibi.api.domain.speechreport.dto.response.SpeechReportReviewRes;
import com.sisibibi.api.domain.speechreport.entity.OffTopicAiReview;
import com.sisibibi.api.domain.speechreport.entity.OffTopicAiReviewStatus;
import com.sisibibi.api.domain.speechreport.entity.SpeechReport;
import com.sisibibi.api.domain.speechreport.entity.SpeechReportReason;
import com.sisibibi.api.domain.speechreport.entity.SpeechReportStatus;
import com.sisibibi.api.domain.speechreport.entity.SpeechReportReviewAction;
import com.sisibibi.api.domain.speechreport.entity.ViolationSeverity;
import com.sisibibi.api.domain.speechreport.repository.OffTopicAiReviewRepository;
import com.sisibibi.api.domain.speechreport.repository.SpeechReportRepository;
import com.sisibibi.api.domain.user.entity.User;
import com.sisibibi.api.domain.user.repository.UserRepository;
import com.sisibibi.api.global.exception.CustomException;
import com.sisibibi.api.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SpeechReportServiceTest {

    @Mock
    private SpeechRepository speechRepository;

    @Mock
    private SpeechReportRepository speechReportRepository;

    @Mock
    private OffTopicAiReviewRepository offTopicAiReviewRepository;

    @Mock
    private OffTopicAiReviewService offTopicAiReviewService;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private SpeechReportService speechReportService;

    @Test
    void getReports_returnsFilteredReportPage() {
        SpeechReport report = createReport(100L, SpeechReportStatus.PENDING);
        PageRequest pageable = PageRequest.of(0, 20);
        given(speechReportRepository.findAllByFilters(
                SpeechReportStatus.PENDING,
                SpeechReportReason.SPAM,
                pageable
        )).willReturn(new PageImpl<>(List.of(report), pageable, 1));
        given(userRepository.findAllById(org.mockito.ArgumentMatchers.any()))
                .willReturn(List.of(user(20L, "신고자"), user(30L, "대상자")));

        Page<SpeechReportSummaryRes> response = speechReportService.getReports(
                SpeechReportStatus.PENDING,
                SpeechReportReason.SPAM,
                pageable
        );

        assertThat(response.getTotalElements()).isEqualTo(1);
        assertThat(response.getContent().getFirst().reportId()).isEqualTo(100L);
        assertThat(response.getContent().getFirst().reportedUserNickname()).isEqualTo("대상자");
        assertThat(response.getContent().getFirst().reporterUserNickname()).isEqualTo("신고자");
        assertThat(response.getContent().getFirst().status()).isEqualTo(SpeechReportStatus.PENDING);
    }

    @Test
    void getReport_returnsReportDetail() {
        SpeechReport report = createReport(100L, SpeechReportStatus.PENDING);
        given(speechReportRepository.findById(100L)).willReturn(Optional.of(report));
        given(offTopicAiReviewRepository.findBySpeechId(10L))
                .willReturn(Optional.of(completedOffTopicAiReview()));
        given(userRepository.findAllById(org.mockito.ArgumentMatchers.any()))
                .willReturn(List.of(user(20L, "신고자"), user(30L, "대상자")));

        SpeechReportDetailRes response = speechReportService.getReport(100L);

        assertThat(response.reportId()).isEqualTo(100L);
        assertThat(response.reportedUserNickname()).isEqualTo("대상자");
        assertThat(response.reporterUserNickname()).isEqualTo("신고자");
        assertThat(response.contentSnapshot()).isEqualTo("신고 대상 의견");
        assertThat(response.reason()).isEqualTo(SpeechReportReason.SPAM);
        assertThat(response.offTopicAiReview()).isNotNull();
        assertThat(response.offTopicAiReview().status())
                .isEqualTo(OffTopicAiReviewStatus.COMPLETED);
        assertThat(response.offTopicAiReview().offTopic()).isTrue();
    }

    @Test
    void getReport_throwsNotFound_whenReportDoesNotExist() {
        given(speechReportRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> speechReportService.getReport(999L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.SPEECH_REPORT_NOT_FOUND);
    }

    @Test
    void reviewReport_startsReview() {
        SpeechReport report = createReport(100L, SpeechReportStatus.PENDING);
        given(speechReportRepository.findByIdForUpdate(100L)).willReturn(Optional.of(report));
        given(userRepository.findAllById(org.mockito.ArgumentMatchers.any()))
                .willReturn(List.of(user(99L, "관리자")));

        SpeechReportReviewRes response = speechReportService.reviewReport(
                100L,
                99L,
                SpeechReportReviewAction.START_REVIEW,
                null,
                null
        );

        assertThat(response.status()).isEqualTo(SpeechReportStatus.REVIEWING);
        assertThat(response.reviewedBy()).isEqualTo(99L);
        assertThat(response.reviewedByNickname()).isEqualTo("관리자");
    }

    @Test
    void reviewReport_resolveOffTopicReport_softDeletesSpeechWithOffTopicReason() {
        SpeechReport report = createReport(100L, SpeechReportStatus.PENDING);
        ReflectionTestUtils.setField(report, "reason", SpeechReportReason.OFF_TOPIC);
        report.review(
                SpeechReportReviewAction.START_REVIEW,
                99L,
                null,
                null,
                LocalDateTime.of(2026, 6, 21, 12, 0)
        );
        Speech speech = Speech.createMainOpinion(1L, 30L, "논점 이탈 의견", SpeechStance.PRO);
        ReflectionTestUtils.setField(speech, "id", 10L);
        given(speechReportRepository.findByIdForUpdate(100L)).willReturn(Optional.of(report));
        given(speechRepository.findByIdAndDeletedFalse(10L)).willReturn(Optional.of(speech));

        speechReportService.reviewReport(
                100L,
                99L,
                SpeechReportReviewAction.RESOLVE,
                "논점 이탈 확인",
                ViolationSeverity.LOW
        );

        assertThat(speech.isDeleted()).isTrue();
        assertThat(speech.getDeleteReason()).isEqualTo(SpeechDeleteReason.OFF_TOPIC);
    }

    @Test
    void reviewReport_throwsNotFound_whenReportDoesNotExist() {
        given(speechReportRepository.findByIdForUpdate(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> speechReportService.reviewReport(
                999L,
                99L,
                SpeechReportReviewAction.START_REVIEW,
                null,
                null
        ))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.SPEECH_REPORT_NOT_FOUND);
    }

    @Test
    void createReport_savesPendingReport() {
        Speech speech = Speech.createMainOpinion(1L, 30L, "신고 대상 의견", SpeechStance.PRO);
        given(speechRepository.findByIdAndDeletedFalse(10L)).willReturn(Optional.of(speech));
        given(speechReportRepository.existsBySpeechIdAndReporterUserId(10L, 20L))
                .willReturn(false);
        given(speechReportRepository.save(org.mockito.ArgumentMatchers.any(SpeechReport.class)))
                .willAnswer(invocation -> {
                    SpeechReport report = invocation.getArgument(0);
                    ReflectionTestUtils.setField(report, "id", 100L);
                    ReflectionTestUtils.setField(report, "createdAt", LocalDateTime.of(2026, 6, 14, 12, 0));
                    return report;
                });

        SpeechReportCreateRes response = speechReportService.createReport(
                10L,
                20L,
                new SpeechReportCreateCommand(
                        SpeechReportReason.HATE_SPEECH,
                        "특정 집단을 비하합니다."
                )
        );

        ArgumentCaptor<SpeechReport> reportCaptor = ArgumentCaptor.forClass(SpeechReport.class);
        verify(speechReportRepository).save(reportCaptor.capture());
        SpeechReport savedReport = reportCaptor.getValue();
        assertThat(savedReport.getSpeechId()).isEqualTo(10L);
        assertThat(savedReport.getReportedUserId()).isEqualTo(30L);
        assertThat(savedReport.getReporterUserId()).isEqualTo(20L);
        assertThat(savedReport.getContentSnapshot()).isEqualTo("신고 대상 의견");
        assertThat(savedReport.getStatus()).isEqualTo(SpeechReportStatus.PENDING);
        assertThat(response.reportId()).isEqualTo(100L);
        assertThat(response.status()).isEqualTo(SpeechReportStatus.PENDING);
    }

    @Test
    void createReport_triggersOffTopicAiReview_whenReasonIsOffTopic() {
        Speech speech = Speech.createMainOpinion(1L, 30L, "논점과 관련 없는 의견", SpeechStance.PRO);
        given(speechRepository.findByIdAndDeletedFalse(10L)).willReturn(Optional.of(speech));
        given(speechReportRepository.existsBySpeechIdAndReporterUserId(10L, 20L))
                .willReturn(false);
        given(speechReportRepository.save(org.mockito.ArgumentMatchers.any(SpeechReport.class)))
                .willAnswer(invocation -> {
                    SpeechReport report = invocation.getArgument(0);
                    ReflectionTestUtils.setField(report, "id", 100L);
                    ReflectionTestUtils.setField(
                            report,
                            "createdAt",
                            LocalDateTime.of(2026, 6, 14, 12, 0)
                    );
                    return report;
                });

        speechReportService.createReport(
                10L,
                20L,
                new SpeechReportCreateCommand(
                        SpeechReportReason.OFF_TOPIC,
                        "논점에서 벗어났습니다."
                )
        );

        verify(offTopicAiReviewService).triggerIfNeeded(
                speech,
                SpeechReportReason.OFF_TOPIC
        );
    }

    @Test
    void createReport_throwsSpeechNotFound_whenSpeechDoesNotExistOrIsDeleted() {
        given(speechRepository.findByIdAndDeletedFalse(10L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> speechReportService.createReport(
                10L,
                20L,
                new SpeechReportCreateCommand(SpeechReportReason.SPAM, null)
        ))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.SPEECH_NOT_FOUND);
    }

    @Test
    void createReport_throwsSelfReportNotAllowed_whenReporterOwnsSpeech() {
        Speech speech = Speech.createMainOpinion(1L, 20L, "내 의견", SpeechStance.CON);
        given(speechRepository.findByIdAndDeletedFalse(10L)).willReturn(Optional.of(speech));

        assertThatThrownBy(() -> speechReportService.createReport(
                10L,
                20L,
                new SpeechReportCreateCommand(SpeechReportReason.OTHER, "잘못 신고")
        ))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.SPEECH_REPORT_SELF_NOT_ALLOWED);

        verify(speechReportRepository, never())
                .save(org.mockito.ArgumentMatchers.any(SpeechReport.class));
    }

    @Test
    void createReport_throwsAlreadyExists_whenReporterAlreadyReportedSpeech() {
        Speech speech = Speech.createMainOpinion(1L, 30L, "신고 대상 의견", SpeechStance.PRO);
        given(speechRepository.findByIdAndDeletedFalse(10L)).willReturn(Optional.of(speech));
        given(speechReportRepository.existsBySpeechIdAndReporterUserId(10L, 20L))
                .willReturn(true);

        assertThatThrownBy(() -> speechReportService.createReport(
                10L,
                20L,
                new SpeechReportCreateCommand(SpeechReportReason.SPAM, null)
        ))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.SPEECH_REPORT_ALREADY_EXISTS);

        verify(speechReportRepository, never())
                .save(org.mockito.ArgumentMatchers.any(SpeechReport.class));
    }

    @Test
    void createReport_throwsDescriptionRequired_whenReasonIsOtherAndDescriptionIsBlank() {
        assertThatThrownBy(() -> speechReportService.createReport(
                10L,
                20L,
                new SpeechReportCreateCommand(SpeechReportReason.OTHER, " ")
        ))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.SPEECH_REPORT_DESCRIPTION_REQUIRED);

        verify(speechRepository, never()).findByIdAndDeletedFalse(10L);
        verify(speechReportRepository, never())
                .save(org.mockito.ArgumentMatchers.any(SpeechReport.class));
    }

    private SpeechReport createReport(Long reportId, SpeechReportStatus status) {
        SpeechReport report = SpeechReport.create(
                10L,
                30L,
                20L,
                "신고 대상 의견",
                SpeechReportReason.SPAM,
                null
        );
        ReflectionTestUtils.setField(report, "id", reportId);
        ReflectionTestUtils.setField(report, "status", status);
        ReflectionTestUtils.setField(report, "createdAt", LocalDateTime.of(2026, 6, 21, 12, 0));
        ReflectionTestUtils.setField(report, "updatedAt", LocalDateTime.of(2026, 6, 21, 12, 0));
        return report;
    }

    private OffTopicAiReview completedOffTopicAiReview() {
        OffTopicAiReview review = OffTopicAiReview.pending(
                10L,
                1L,
                "신고된 의견",
                5,
                5,
                50
        );
        review.complete(new OffTopicAiReviewResult(
                true,
                "논점 이탈입니다.",
                0.9
        ), LocalDateTime.of(2026, 6, 21, 12, 5));
        ReflectionTestUtils.setField(review, "id", 200L);
        return review;
    }

    private User user(Long id, String nickname) {
        User user = User.signup("user" + id + "@example.com", "password", nickname);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}
