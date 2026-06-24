package com.sisibibi.api.domain.report.service;

import com.sisibibi.api.domain.report.dto.event.AiReportGenerationRequestedEvent;
import com.sisibibi.api.domain.report.dto.request.AiReportGenerateReq;
import com.sisibibi.api.domain.report.dto.response.AiReportRes;
import com.sisibibi.api.domain.report.prompt.CustomPromptCommand;
import com.sisibibi.api.domain.report.prompt.CustomPromptValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AiReportService {

    private final AiReportPersistenceService aiReportPersistenceService;
    private final CustomPromptValidator customPromptValidator;
    private final ApplicationEventPublisher eventPublisher;

    public AiReportRes generateReport(Long roomId) {
        return generateReport(roomId, AiReportGenerateReq.empty());
    }

    public AiReportRes generateReport(Long roomId, AiReportGenerateReq request) {
        return generateReportInternal(roomId, null, request, false);
    }

    public AiReportRes generateReport(Long roomId, Long userId, AiReportGenerateReq request) {
        return generateReportInternal(roomId, userId, request, true);
    }

    private AiReportRes generateReportInternal(
            Long roomId,
            Long userId,
            AiReportGenerateReq request,
            boolean userScopedCustomReports
    ) {
        List<CustomPromptCommand> customPrompts = customPromptValidator.normalizeAndScan(request);
        AiReportRequestResult result = userScopedCustomReports
                ? aiReportPersistenceService.requestGeneration(roomId, userId, customPrompts)
                : aiReportPersistenceService.requestGeneration(roomId, customPrompts);

        if (result.shouldPublish()) {
            eventPublisher.publishEvent(new AiReportGenerationRequestedEvent(
                    result.response().reportId(),
                    result.response().roomId(),
                    result.generationType()
            ));
        }

        return result.response();
    }

    public AiReportRes getReport(Long roomId) {
        return aiReportPersistenceService.getReport(roomId);
    }

    public AiReportRes getReport(Long roomId, Long userId) {
        return aiReportPersistenceService.getReport(roomId, userId);
    }

}
