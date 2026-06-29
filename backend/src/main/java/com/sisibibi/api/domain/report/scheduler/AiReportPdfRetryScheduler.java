package com.sisibibi.api.domain.report.scheduler;

import com.sisibibi.api.domain.report.entity.AiReportNotificationStatus;
import com.sisibibi.api.domain.report.entity.AiReportPdfStatus;
import com.sisibibi.api.domain.report.repository.AiReportPdfExportRepository;
import com.sisibibi.api.domain.report.service.AiReportPdfGenerationService;
import com.sisibibi.api.domain.report.service.AiReportPdfProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiReportPdfRetryScheduler {

    private final AiReportPdfExportRepository exportRepository;
    private final AiReportPdfGenerationService generationService;
    private final AiReportPdfProperties properties;

    @Scheduled(fixedDelayString = "${app.ai-report.pdf.retry-fixed-delay-ms:60000}")
    public void retryFailedNotifications() {
        if (!properties.isEnabled()) {
            return;
        }
        LocalDateTime attemptedBefore = LocalDateTime.now().minus(properties.getRetryStaleThreshold());
        exportRepository.findNotificationRetryCandidates(
                AiReportPdfStatus.READY,
                AiReportNotificationStatus.FAILED,
                attemptedBefore,
                properties.getMaxNotificationRetryCount(),
                PageRequest.of(0, properties.getRetryBatchSize())
        ).forEach(export -> retryNotificationQuietly(export.getId()));
    }

    private void retryNotificationQuietly(Long exportId) {
        try {
            generationService.retryNotification(exportId);
        } catch (RuntimeException e) {
            log.warn("Failed to retry AI report PDF notification. exportId={}", exportId, e);
        }
    }
}
