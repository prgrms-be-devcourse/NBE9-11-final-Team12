package com.sisibibi.api.domain.report.repository;

import com.sisibibi.api.domain.report.entity.AiReportNotificationStatus;
import com.sisibibi.api.domain.report.entity.AiReportPdfExport;
import com.sisibibi.api.domain.report.entity.AiReportPdfStatus;
import com.sisibibi.api.domain.report.entity.AiReportPdfType;
import com.sisibibi.api.global.config.JpaAuditingConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@Import(JpaAuditingConfig.class)
class AiReportPdfExportRepositoryTest {

    @Autowired
    private AiReportPdfExportRepository repository;

    @Test
    void save_initializesRequesterOwnedExport() {
        AiReportPdfExport saved = repository.saveAndFlush(
                AiReportPdfExport.notStarted(10L, 20L, 7L, AiReportPdfType.BASE)
        );

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getAiReportId()).isEqualTo(10L);
        assertThat(saved.getRoomId()).isEqualTo(20L);
        assertThat(saved.getRequestedByUserId()).isEqualTo(7L);
        assertThat(saved.getPdfType()).isEqualTo(AiReportPdfType.BASE);
        assertThat(saved.getPdfStatus()).isEqualTo(AiReportPdfStatus.NOT_STARTED);
        assertThat(saved.getNotificationStatus()).isEqualTo(AiReportNotificationStatus.NOT_SENT);
        assertThat(saved.getPdfRetryCount()).isZero();
        assertThat(saved.getNotificationRetryCount()).isZero();
    }

    @Test
    void findByAiReportIdAndRequestedByUserId_returnsExport() {
        AiReportPdfExport export = repository.saveAndFlush(
                AiReportPdfExport.notStarted(10L, 20L, 7L, AiReportPdfType.BASE)
        );

        assertThat(repository.findByAiReportIdAndRequestedByUserIdAndPdfType(10L, 7L, AiReportPdfType.BASE))
                .contains(export);
    }

    @Test
    void allowsSeparateBaseAndCustomExportsForSameReportAndUser() {
        AiReportPdfExport base = repository.saveAndFlush(
                AiReportPdfExport.notStarted(10L, 20L, 7L, AiReportPdfType.BASE)
        );
        AiReportPdfExport custom = repository.saveAndFlush(
                AiReportPdfExport.notStarted(10L, 20L, 7L, AiReportPdfType.CUSTOM)
        );

        assertThat(repository.findByAiReportIdAndRequestedByUserIdAndPdfType(10L, 7L, AiReportPdfType.BASE))
                .contains(base);
        assertThat(repository.findByAiReportIdAndRequestedByUserIdAndPdfType(10L, 7L, AiReportPdfType.CUSTOM))
                .contains(custom);
    }

    @Test
    void findPdfRetryCandidates_returnsFailedExportsBelowLimitAndStaleEnough() {
        AiReportPdfExport retryable = repository.saveAndFlush(AiReportPdfExport.notStarted(10L, 20L, 7L));
        retryable.markGenerating(LocalDateTime.of(2026, 6, 28, 12, 0));
        retryable.markPdfFailed("render failed", LocalDateTime.of(2026, 6, 28, 12, 1));

        AiReportPdfExport tooNew = repository.saveAndFlush(AiReportPdfExport.notStarted(11L, 21L, 8L));
        tooNew.markGenerating(LocalDateTime.of(2026, 6, 28, 12, 2));
        tooNew.markPdfFailed("s3 failed", LocalDateTime.of(2026, 6, 28, 12, 5));

        AiReportPdfExport retryLimitReached = repository.saveAndFlush(AiReportPdfExport.notStarted(12L, 22L, 9L));
        retryLimitReached.markGenerating(LocalDateTime.of(2026, 6, 28, 11, 50));
        retryLimitReached.markPdfFailed("first failed", LocalDateTime.of(2026, 6, 28, 11, 51));
        retryLimitReached.markPdfFailed("second failed", LocalDateTime.of(2026, 6, 28, 11, 52));
        retryLimitReached.markPdfFailed("third failed", LocalDateTime.of(2026, 6, 28, 11, 53));

        repository.flush();

        List<AiReportPdfExport> results = repository.findPdfRetryCandidates(
                AiReportPdfStatus.FAILED,
                LocalDateTime.of(2026, 6, 28, 12, 3),
                3,
                PageRequest.of(0, 10)
        );

        assertThat(results).extracting(AiReportPdfExport::getId)
                .containsExactly(retryable.getId());
    }

    @Test
    void findNotificationRetryCandidates_returnsReadyNotificationFailuresBelowLimitAndStaleEnough() {
        AiReportPdfExport retryable = repository.saveAndFlush(AiReportPdfExport.notStarted(10L, 20L, 7L));
        retryable.markGenerating(LocalDateTime.of(2026, 6, 28, 12, 0));
        retryable.markPdfReady("ai-reports/20/10/7.pdf", LocalDateTime.of(2026, 6, 28, 12, 1));
        retryable.markNotificationFailed("smtp failed", LocalDateTime.of(2026, 6, 28, 12, 2));

        AiReportPdfExport pendingPdf = repository.saveAndFlush(AiReportPdfExport.notStarted(11L, 21L, 8L));
        pendingPdf.markNotificationFailed("smtp failed", LocalDateTime.of(2026, 6, 28, 12, 1));

        AiReportPdfExport tooNew = repository.saveAndFlush(AiReportPdfExport.notStarted(12L, 22L, 9L));
        tooNew.markGenerating(LocalDateTime.of(2026, 6, 28, 12, 3));
        tooNew.markPdfReady("ai-reports/22/12/9.pdf", LocalDateTime.of(2026, 6, 28, 12, 4));
        tooNew.markNotificationFailed("smtp failed", LocalDateTime.of(2026, 6, 28, 12, 5));

        AiReportPdfExport retryLimitReached = repository.saveAndFlush(AiReportPdfExport.notStarted(13L, 23L, 10L));
        retryLimitReached.markGenerating(LocalDateTime.of(2026, 6, 28, 11, 50));
        retryLimitReached.markPdfReady("ai-reports/23/13/10.pdf", LocalDateTime.of(2026, 6, 28, 11, 51));
        retryLimitReached.markNotificationFailed("first failed", LocalDateTime.of(2026, 6, 28, 11, 52));
        retryLimitReached.markNotificationFailed("second failed", LocalDateTime.of(2026, 6, 28, 11, 53));
        retryLimitReached.markNotificationFailed("third failed", LocalDateTime.of(2026, 6, 28, 11, 54));
        retryLimitReached.markNotificationFailed("fourth failed", LocalDateTime.of(2026, 6, 28, 11, 55));
        retryLimitReached.markNotificationFailed("fifth failed", LocalDateTime.of(2026, 6, 28, 11, 56));

        repository.flush();

        List<AiReportPdfExport> results = repository.findNotificationRetryCandidates(
                AiReportPdfStatus.READY,
                AiReportNotificationStatus.FAILED,
                LocalDateTime.of(2026, 6, 28, 12, 3),
                5,
                PageRequest.of(0, 10)
        );

        assertThat(results).extracting(AiReportPdfExport::getId)
                .containsExactly(retryable.getId());
    }

    @Test
    void findNotificationPendingCandidates_returnsReadyUnsentExportsStaleEnough() {
        AiReportPdfExport retryable = repository.saveAndFlush(AiReportPdfExport.notStarted(20L, 30L, 17L));
        retryable.markGenerating(LocalDateTime.of(2026, 6, 28, 12, 0));
        retryable.markPdfReady("ai-reports/30/20/17.pdf", LocalDateTime.of(2026, 6, 28, 12, 1));

        AiReportPdfExport pendingPdf = repository.saveAndFlush(AiReportPdfExport.notStarted(21L, 31L, 18L));

        AiReportPdfExport tooNew = repository.saveAndFlush(AiReportPdfExport.notStarted(22L, 32L, 19L));
        tooNew.markGenerating(LocalDateTime.of(2026, 6, 28, 12, 3));
        tooNew.markPdfReady("ai-reports/32/22/19.pdf", LocalDateTime.of(2026, 6, 28, 12, 5));

        AiReportPdfExport alreadyFailed = repository.saveAndFlush(AiReportPdfExport.notStarted(23L, 33L, 20L));
        alreadyFailed.markGenerating(LocalDateTime.of(2026, 6, 28, 12, 0));
        alreadyFailed.markPdfReady("ai-reports/33/23/20.pdf", LocalDateTime.of(2026, 6, 28, 12, 1));
        alreadyFailed.markNotificationFailed("smtp failed", LocalDateTime.of(2026, 6, 28, 12, 2));

        repository.flush();

        List<AiReportPdfExport> results = repository.findNotificationPendingCandidates(
                AiReportPdfStatus.READY,
                AiReportNotificationStatus.NOT_SENT,
                LocalDateTime.of(2026, 6, 28, 12, 3),
                5,
                PageRequest.of(0, 10)
        );

        assertThat(results).extracting(AiReportPdfExport::getId)
                .containsExactly(retryable.getId());
    }
}
