package com.sisibibi.api.domain.report.service;

import com.sisibibi.api.domain.report.dto.event.AiReportGenerationRequestedEvent;
import com.sisibibi.api.domain.report.dto.request.AiReportGenerateReq;
import com.sisibibi.api.domain.report.dto.response.AiReportRes;
import com.sisibibi.api.domain.report.entity.AiReportPdfExport;
import com.sisibibi.api.domain.report.outbox.AiReportPdfGenerationOutboxWriter;
import com.sisibibi.api.domain.report.prompt.CustomPromptCommand;
import com.sisibibi.api.domain.report.prompt.CustomPromptValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AiReportService {

    private final AiReportPersistenceService aiReportPersistenceService;
    private final CustomPromptValidator customPromptValidator;
    private final ApplicationEventPublisher eventPublisher;
    private final AiReportPdfPersistenceService aiReportPdfPersistenceService;
    private final AiReportPdfGenerationOutboxWriter outboxWriter;

    public AiReportRes generateReport(Long roomId, Long userId) {
        return generateReport(roomId, userId, AiReportGenerateReq.empty());
    }

    public AiReportRes generateReport(Long roomId, Long userId, AiReportGenerateReq request) {
        List<CustomPromptCommand> customPrompts = customPromptValidator.normalizeAndScan(request);
        AiReportRequestResult result = aiReportPersistenceService.requestGeneration(roomId, userId, customPrompts);

        if (result.shouldPublish()) {
            eventPublisher.publishEvent(new AiReportGenerationRequestedEvent(
                    result.response().reportId(),
                    result.response().roomId(),
                    result.generationType()
            ));
        }

        AiReportPdfExport export = aiReportPdfPersistenceService.createIfMissing(
                result.response().reportId(),
                result.response().roomId(),
                userId
        );
        if ("COMPLETED".equals(result.response().status()) && export.shouldStartGeneration()) {
            outboxWriter.record(export.getId(), LocalDateTime.now());
        }

        return result.response();
    }

    public AiReportRes getReport(Long roomId, Long userId) {
        return aiReportPersistenceService.getReport(roomId, userId);
    }

}
