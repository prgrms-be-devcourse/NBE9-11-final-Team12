package com.sisibibi.api.domain.report.entity;

import static org.assertj.core.api.Assertions.assertThat;

import com.sisibibi.api.domain.report.client.dto.AiReportCustomReportPayload;
import com.sisibibi.api.domain.report.client.dto.AiReportGenerateRes;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class AiReportTest {

    @Test
    void requested_usesEmptyCustomPrompts_whenCustomPromptsIsNull() {
        AiReport report = AiReport.requested(1L, null);

        assertThat(report.getRoomId()).isEqualTo(1L);
        assertThat(report.getStatus()).isEqualTo(AiReportStatus.REQUESTED);
        assertThat(report.getCustomPrompts()).isEmpty();
        assertThat(report.getCustomReports()).isEmpty();
        assertThat(report.getRequestedAt()).isNotNull();
    }

    @Test
    void shouldSkipGeneration_returnsTrue_whenReportIsInProgressOrCompleted() {
        AiReport requested = AiReport.requested(1L);
        AiReport queued = AiReport.requested(1L);
        queued.markQueued();
        AiReport processing = AiReport.requested(1L);
        processing.markProcessing(LocalDateTime.of(2026, 6, 28, 1, 0), Duration.ofMinutes(1));
        AiReport completed = completedReport();

        assertThat(requested.shouldSkipGeneration()).isTrue();
        assertThat(queued.shouldSkipGeneration()).isTrue();
        assertThat(processing.shouldSkipGeneration()).isTrue();
        assertThat(completed.shouldSkipGeneration()).isTrue();
    }

    @Test
    void shouldSkipGeneration_returnsFalse_whenReportIsFailed() {
        AiReport generationFailed = AiReport.requested(1L);
        generationFailed.fail("generation failed");
        AiReport publishFailed = AiReport.requested(1L);
        publishFailed.markPublishFailed("PUBLISH_ERROR", "publish failed");

        assertThat(generationFailed.shouldSkipGeneration()).isFalse();
        assertThat(publishFailed.shouldSkipGeneration()).isFalse();
    }

    @Test
    void hasBaseReportContent_returnsFalse_whenAnyBaseFieldIsMissing() {
        AiReport report = completedReport();

        ReflectionTestUtils.setField(report, "keyIssues", null);

        assertThat(report.hasBaseReportContent()).isFalse();
    }

    @Test
    void hasBaseReportContent_returnsTrue_whenAllBaseFieldsExist() {
        AiReport report = completedReport();

        assertThat(report.hasBaseReportContent()).isTrue();
    }

    @Test
    void markProcessing_usesCurrentTimeAndDefaultLockDuration_whenArgumentsAreNull() {
        AiReport report = AiReport.requested(1L);
        LocalDateTime before = LocalDateTime.now().minusSeconds(1);

        report.markProcessing(null, null);

        LocalDateTime after = LocalDateTime.now().plusSeconds(1);
        assertThat(report.getStatus()).isEqualTo(AiReportStatus.PROCESSING);
        assertThat(report.getProcessingStartedAt()).isBetween(before, after);
        assertThat(report.getProcessingLockedUntil())
                .isEqualTo(report.getProcessingStartedAt().plusMinutes(5));
        assertThat(report.getFailedAt()).isNull();
    }

    @Test
    void markProcessing_usesProvidedTimeAndLockDuration() {
        AiReport report = AiReport.requested(1L);
        LocalDateTime startedAt = LocalDateTime.of(2026, 6, 28, 1, 0);

        report.markProcessing(startedAt, Duration.ofMinutes(2));

        assertThat(report.getProcessingStartedAt()).isEqualTo(startedAt);
        assertThat(report.getProcessingLockedUntil()).isEqualTo(startedAt.plusMinutes(2));
    }

    @Test
    void markPublishFailed_truncatesErrorFieldsAndIncrementsRetryCount() {
        AiReport report = AiReport.requested(1L);
        String longCode = "E".repeat(101);
        String longMessage = "M".repeat(1001);

        report.markPublishFailed(longCode, longMessage);

        assertThat(report.getStatus()).isEqualTo(AiReportStatus.PUBLISH_FAILED);
        assertThat(report.getPublishRetryCount()).isEqualTo(1);
        assertThat(report.getLastErrorCode()).hasSize(100);
        assertThat(report.getLastErrorMessage()).hasSize(1000);
        assertThat(report.getFailedAt()).isNotNull();
    }

    @Test
    void fail_allowsNullErrorAndTruncatesLongErrorFields() {
        AiReport report = AiReport.requested(1L);
        String longCode = "E".repeat(101);
        String longMessage = "M".repeat(1001);

        report.fail(longCode, longMessage);

        assertThat(report.getStatus()).isEqualTo(AiReportStatus.GENERATION_FAILED);
        assertThat(report.getGenerationRetryCount()).isEqualTo(1);
        assertThat(report.getLastErrorCode()).hasSize(100);
        assertThat(report.getLastErrorMessage()).hasSize(1000);
        assertThat(report.getErrorMessage()).hasSize(1000);

        report.fail(null, null);

        assertThat(report.getLastErrorCode()).isNull();
        assertThat(report.getLastErrorMessage()).isNull();
        assertThat(report.getErrorMessage()).isNull();
        assertThat(report.getGenerationRetryCount()).isEqualTo(2);
    }

    @Test
    void complete_usesFallbackPrompt_whenCustomReportCountExceedsPromptCount() {
        AiReport report = AiReport.requested(1L, List.of(
                new AiReportCustomPrompt("custom 1", "prompt 1")
        ));

        report.complete(new AiReportGenerateRes(
                "core",
                List.of("issue"),
                "summary",
                "common",
                "opinion",
                List.of(
                        new AiReportCustomReportPayload("result 1", "content 1"),
                        new AiReportCustomReportPayload("result 2", "content 2")
                )
        ));

        assertThat(report.getCustomReports()).containsExactly(
                new AiReportCustomReport("custom 1", "prompt 1", "result 1", "content 1"),
                new AiReportCustomReport("custom 2", "", "result 2", "content 2")
        );
    }

    @Test
    void complete_usesEmptyCustomReports_whenResponseCustomReportsIsNull() {
        AiReport report = AiReport.requested(1L);

        report.complete(new AiReportGenerateRes(
                "core",
                List.of("issue"),
                "summary",
                "common",
                "opinion",
                null
        ));

        assertThat(report.getCustomReports()).isEmpty();
    }

    @Test
    void completeCustomReports_mergesExistingReportsAndKeepsBaseContent() {
        AiReport report = completedReport();
        report.requestCustomReports(List.of(new AiReportCustomPrompt("custom 1", "prompt 1")));

        report.completeCustomReports(new AiReportGenerateRes(
                null,
                null,
                null,
                null,
                null,
                List.of(new AiReportCustomReportPayload("result 1", "content 1"))
        ));

        assertThat(report.getStatus()).isEqualTo(AiReportStatus.COMPLETED);
        assertThat(report.getCustomReports()).containsExactly(
                new AiReportCustomReport("custom 1", "prompt 1", "result 1", "content 1")
        );
        assertThat(report.getCompletedAt()).isNotNull();
    }

    @Test
    void appendCustomReports_preservesExistingWhenNewPromptAndReportsAreEmpty() {
        AiReport report = completedReport();
        report.appendCustomReports(
                List.of(new AiReportCustomPrompt("custom 1", "prompt 1")),
                List.of(new AiReportCustomReportPayload("result 1", "content 1"))
        );

        report.appendCustomReports(null, null);

        assertThat(report.getCustomPrompts()).containsExactly(
                new AiReportCustomPrompt("custom 1", "prompt 1")
        );
        assertThat(report.getCustomReports()).containsExactly(
                new AiReportCustomReport("custom 1", "prompt 1", "result 1", "content 1")
        );
    }

    @Test
    void appendCustomReports_usesViewerUserId_whenPromptUserIdIsNull() {
        AiReport report = completedReport();

        report.appendCustomReports(
                7L,
                List.of(new AiReportCustomPrompt("custom 1", "prompt 1")),
                List.of(new AiReportCustomReportPayload("result 1", "content 1"))
        );

        assertThat(report.getCustomReports()).containsExactly(
                new AiReportCustomReport(7L, "custom 1", "prompt 1", "result 1", "content 1")
        );
    }

    @Test
    void appendCustomReports_keepsPromptUserId_whenPromptUserIdExists() {
        AiReport report = completedReport();

        report.appendCustomReports(
                7L,
                List.of(new AiReportCustomPrompt(8L, "custom 1", "prompt 1")),
                List.of(new AiReportCustomReportPayload("result 1", "content 1"))
        );

        assertThat(report.getCustomReports()).containsExactly(
                new AiReportCustomReport(8L, "custom 1", "prompt 1", "result 1", "content 1")
        );
    }

    @Test
    void appendCustomReports_usesFallbackPromptWithViewerUserId_whenReportCountExceedsPromptCount() {
        AiReport report = completedReport();

        report.appendCustomReports(
                7L,
                List.of(),
                List.of(new AiReportCustomReportPayload("result 1", "content 1"))
        );

        assertThat(report.getCustomReports()).containsExactly(
                new AiReportCustomReport(7L, "custom 1", "", "result 1", "content 1")
        );
    }

    @Test
    void requestCustomReports_resetsPublishFailureStateAndUsesEmptyPromptsWhenNull() {
        AiReport report = AiReport.requested(1L);
        report.markPublishFailed("PUBLISH_ERROR", "failed");

        report.requestCustomReports(null);

        assertThat(report.getStatus()).isEqualTo(AiReportStatus.REQUESTED);
        assertThat(report.getCustomPrompts()).isEmpty();
        assertThat(report.getPublishRetryCount()).isZero();
        assertThat(report.getLastErrorCode()).isNull();
        assertThat(report.getLastErrorMessage()).isNull();
        assertThat(report.getFailedAt()).isNull();
    }

    @Test
    void rememberCustomPrompts_usesEmptyPromptsWhenNullAndClearsLastError() {
        AiReport report = AiReport.requested(1L);
        report.markPublishFailed("PUBLISH_ERROR", "failed");

        report.rememberCustomPrompts(null);

        assertThat(report.getCustomPrompts()).isEmpty();
        assertThat(report.getLastErrorCode()).isNull();
        assertThat(report.getLastErrorMessage()).isNull();
    }

    @Test
    void retry_resetsBaseAndCustomReportContent() {
        AiReport report = completedReport();

        report.retry();

        assertThat(report.getStatus()).isEqualTo(AiReportStatus.REQUESTED);
        assertThat(report.getCoreLine()).isNull();
        assertThat(report.getKeyIssues()).isEmpty();
        assertThat(report.getCustomPrompts()).isEmpty();
        assertThat(report.getCustomReports()).isEmpty();
        assertThat(report.getAiSummary()).isNull();
        assertThat(report.getCommonGround()).isNull();
        assertThat(report.getAiOpinion()).isNull();
        assertThat(report.getCompletedAt()).isNull();
    }

    private AiReport completedReport() {
        AiReport report = AiReport.requested(1L);
        report.complete(new AiReportGenerateRes(
                "core",
                List.of("issue"),
                "summary",
                "common",
                "opinion"
        ));
        return report;
    }
}
