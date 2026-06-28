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
    public void retryFailedExports() {
        if (!properties.isEnabled()) {
            return;
        }
        LocalDateTime attemptedBefore = LocalDateTime.now().minus(properties.getRetryStaleThreshold());
        exportRepository.findPdfRetryCandidates(
                AiReportPdfStatus.FAILED,
                attemptedBefore,
                properties.getMaxPdfRetryCount(),
                PageRequest.of(0, properties.getRetryBatchSize())
        ).forEach(export -> generationService.generate(export.getId()));

        exportRepository.findNotificationRetryCandidates(
                AiReportPdfStatus.READY,
                AiReportNotificationStatus.FAILED,
                attemptedBefore,
                properties.getMaxNotificationRetryCount(),
                PageRequest.of(0, properties.getRetryBatchSize())
        ).forEach(export -> generationService.generate(export.getId()));
    }
}
