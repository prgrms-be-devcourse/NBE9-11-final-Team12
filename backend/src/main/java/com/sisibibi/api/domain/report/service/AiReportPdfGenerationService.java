package com.sisibibi.api.domain.report.service;

import com.sisibibi.api.domain.report.entity.AiReportPdfExport;
import com.sisibibi.api.domain.report.notification.AiReportNotificationProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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

    public void generate(Long exportId) {
        AiReportPdfExport export = persistenceService.prepareGeneration(exportId);
        AiReportPdfModel model;
        try {
            model = dataCollector.collect(export);
            byte[] pdfBytes = renderer.render(model);
            String objectKey = storage.upload(model.roomId(), model.reportId(), export.getRequestedByUserId(), pdfBytes);
            persistenceService.completePdf(exportId, objectKey);
        } catch (RuntimeException e) {
            log.warn("AI report PDF generation failed. exportId={}", exportId, e);
            persistenceService.failPdf(exportId, e.getMessage());
            return;
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
            log.warn("AI report PDF notification failed. exportId={}", exportId, e);
            persistenceService.markNotificationFailed(exportId, e.getMessage());
        }
    }
}
