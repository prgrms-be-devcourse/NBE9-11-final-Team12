package com.sisibibi.api.domain.report.service;

import com.sisibibi.api.domain.report.client.AiReportClient;
import com.sisibibi.api.domain.report.client.dto.AiReportGenerateRes;
import com.sisibibi.api.domain.report.dto.response.AiReportRes;
import com.sisibibi.api.global.exception.CustomException;
import com.sisibibi.api.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AiReportService {

    private final AiReportPersistenceService aiReportPersistenceService;
    private final AiReportClient aiReportClient;

    public AiReportRes generateReport(Long roomId) {
        AiReportGenerationContext context = aiReportPersistenceService.prepareGeneration(roomId);

        if (!context.shouldCallAi()) {
            return context.response();
        }

        try {
            AiReportGenerateRes response = aiReportClient.generate(context.request());

            if (!response.hasRequiredFields()) {
                return aiReportPersistenceService.fail(
                        context.reportId(),
                        ErrorCode.AI_REPORT_INVALID_RESPONSE.getMessage()
                );
            }

            return aiReportPersistenceService.complete(context.reportId(), response);
        } catch (CustomException e) {
            return aiReportPersistenceService.fail(context.reportId(), e.getErrorCode().getMessage());
        } catch (RuntimeException e) {
            return aiReportPersistenceService.fail(
                    context.reportId(),
                    ErrorCode.AI_REPORT_GENERATE_FAILED.getMessage()
            );
        }
    }

    public AiReportRes getReport(Long roomId) {
        return aiReportPersistenceService.getReport(roomId);
    }
}
