package com.sisibibi.api.domain.speechreport.entity;

import com.sisibibi.api.global.exception.CustomException;
import com.sisibibi.api.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SpeechReportTest {

    @Test
    void create_setsPendingStatusAndNormalizesBlankDescription() {
        SpeechReport report = SpeechReport.create(
                10L,
                30L,
                20L,
                "신고 당시 의견",
                SpeechReportReason.ABUSE_HARASSMENT,
                " "
        );

        assertThat(report.getSpeechId()).isEqualTo(10L);
        assertThat(report.getReportedUserId()).isEqualTo(30L);
        assertThat(report.getReporterUserId()).isEqualTo(20L);
        assertThat(report.getContentSnapshot()).isEqualTo("신고 당시 의견");
        assertThat(report.getReason()).isEqualTo(SpeechReportReason.ABUSE_HARASSMENT);
        assertThat(report.getDescription()).isNull();
        assertThat(report.getStatus()).isEqualTo(SpeechReportStatus.PENDING);
    }

    @Test
    void reportReasons_includeFrontendModerationCategories() {
        assertThat(SpeechReportReason.values()).contains(
                SpeechReportReason.MISINFORMATION,
                SpeechReportReason.PRIVACY_VIOLATION,
                SpeechReportReason.OFF_TOPIC
        );
    }

    @Test
    void review_transitionsPendingToReviewing() {
        SpeechReport report = createReport();

        report.review(SpeechReportReviewAction.START_REVIEW, 99L, null, null);

        assertThat(report.getStatus()).isEqualTo(SpeechReportStatus.REVIEWING);
        assertThat(report.getReviewedBy()).isEqualTo(99L);
        assertThat(report.getReviewedAt()).isNull();
    }

    @Test
    void review_resolvesReviewingReportAndNormalizesNote() {
        SpeechReport report = createReport();
        LocalDateTime reviewedAt = LocalDateTime.of(2026, 6, 21, 13, 0);
        report.review(SpeechReportReviewAction.START_REVIEW, 99L, null, null);

        report.review(SpeechReportReviewAction.RESOLVE, 100L, "  위반 사항 확인  ", reviewedAt);

        assertThat(report.getStatus()).isEqualTo(SpeechReportStatus.RESOLVED);
        assertThat(report.getReviewedBy()).isEqualTo(100L);
        assertThat(report.getReviewedAt()).isEqualTo(reviewedAt);
        assertThat(report.getResolutionNote()).isEqualTo("위반 사항 확인");
    }

    @Test
    void review_rejectsReviewingReport() {
        SpeechReport report = createReport();
        LocalDateTime reviewedAt = LocalDateTime.of(2026, 6, 21, 13, 0);
        report.review(SpeechReportReviewAction.START_REVIEW, 99L, null, null);

        report.review(SpeechReportReviewAction.REJECT, 99L, "위반 사항 없음", reviewedAt);

        assertThat(report.getStatus()).isEqualTo(SpeechReportStatus.REJECTED);
        assertThat(report.getReviewedAt()).isEqualTo(reviewedAt);
    }

    @Test
    void review_throwsInvalidTransition_whenResolvingPendingReport() {
        SpeechReport report = createReport();

        assertThatThrownBy(() -> report.review(
                SpeechReportReviewAction.RESOLVE,
                99L,
                "처리 완료",
                LocalDateTime.now()
        ))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.SPEECH_REPORT_INVALID_STATUS_TRANSITION);
    }

    @Test
    void review_throwsResolutionNoteRequired_whenNoteIsBlank() {
        SpeechReport report = createReport();
        report.review(SpeechReportReviewAction.START_REVIEW, 99L, null, null);

        assertThatThrownBy(() -> report.review(
                SpeechReportReviewAction.RESOLVE,
                99L,
                " ",
                LocalDateTime.now()
        ))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.SPEECH_REPORT_RESOLUTION_NOTE_REQUIRED);
    }

    @Test
    void review_throwsResolutionNoteTooLong_whenNoteExceedsLimit() {
        SpeechReport report = createReport();
        report.review(SpeechReportReviewAction.START_REVIEW, 99L, null, null);

        assertThatThrownBy(() -> report.review(
                SpeechReportReviewAction.RESOLVE,
                99L,
                "a".repeat(501),
                LocalDateTime.now()
        ))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.SPEECH_REPORT_RESOLUTION_NOTE_TOO_LONG);
    }

    private SpeechReport createReport() {
        return SpeechReport.create(
                10L,
                30L,
                20L,
                "신고 당시 의견",
                SpeechReportReason.SPAM,
                null
        );
    }
}
