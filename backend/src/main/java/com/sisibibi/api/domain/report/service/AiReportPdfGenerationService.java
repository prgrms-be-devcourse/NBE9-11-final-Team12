package com.sisibibi.api.domain.report.service;

import com.sisibibi.api.domain.report.entity.AiReportPdfExport;
import com.sisibibi.api.domain.report.notification.AiReportNotificationProperties;
import com.sisibibi.api.domain.report.outbox.AiReportPdfNotificationOutboxWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiReportPdfGenerationService {

    private final AiReportPdfPersistenceService persistenceService;
    private final AiReportPdfDataCollector dataCollector;
    private final AiReportPdfRenderer renderer;
    private final AiReportPdfStorage storage;
    private final AiReportNotificationSender notificationSender;
    private final AiReportNotificationProperties notificationProperties;
    private final AiReportPdfNotificationOutboxWriter notificationOutboxWriter;

    private static final int MAX_ATTEMPTS = 3;
    private static final long RETRY_DELAY_MS = 100;

    public void generate(Long exportId) {
        AiReportPdfExport export = persistenceService.prepareGeneration(exportId);
        AiReportPdfModel model = generateWithRetry(exportId, export);
        sendNotificationQuietly(exportId, model);
    }

    private AiReportPdfModel generateWithRetry(Long exportId, AiReportPdfExport export) {
        RuntimeException lastException = null;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                AiReportPdfModel model = dataCollector.collect(export);
                byte[] pdfBytes = renderer.render(model);
                String objectKey = storage.upload(
                        model.roomId(),
                        model.reportId(),
                        export.getRequestedByUserId(),
                        export.getPdfType(),
                        pdfBytes
                );
                persistenceService.completePdf(exportId, objectKey);
                return model;
            } catch (RuntimeException e) {
                lastException = e;
                log.warn("AI report PDF generation attempt {}/{} failed. exportId={}", attempt, MAX_ATTEMPTS, exportId, e);
                if (attempt < MAX_ATTEMPTS) {
                    sleep(RETRY_DELAY_MS);
                }
            }
        }
        persistenceService.failPdf(exportId, lastException.getMessage());
        throw lastException;
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void retryNotification(Long exportId) {
        AiReportPdfExport export = persistenceService.loadExport(exportId);
        AiReportPdfModel model;
        try {
            model = dataCollector.collect(export);
        } catch (RuntimeException e) {
            log.warn("Failed to collect data for PDF notification retry. exportId={}", exportId, e);
            persistenceService.markNotificationFailed(exportId, e.getMessage());
            throw e;
        }
        try {
            notificationSender.sendPdfReady(new AiReportPdfReadyCommand(
                    exportId,
                    model.requesterEmail(),
                    model.requesterNickname(),
                    model.roomTitle(),
                    notificationProperties.getHomepageUrl()
            ));
            persistenceService.markNotificationSent(exportId);
        } catch (RuntimeException e) {
            log.warn("AI report PDF notification retry failed. exportId={}", exportId, e);
            persistenceService.markNotificationFailed(exportId, e.getMessage());
            throw e;
        }
    }

    private void sendNotificationQuietly(Long exportId, AiReportPdfModel model) {
        try {
            notificationSender.sendPdfReady(new AiReportPdfReadyCommand(
                    exportId,
                    model.requesterEmail(),
                    model.requesterNickname(),
                    model.roomTitle(),
                    notificationProperties.getHomepageUrl()
            ));
            persistenceService.markNotificationSent(exportId);
        } catch (RuntimeException e) {
            log.warn("AI report PDF notification failed. exportId={}", exportId, e);
            persistenceService.markNotificationFailed(exportId, e.getMessage());
            notificationOutboxWriter.record(exportId, LocalDateTime.now());
        }
    }
}
