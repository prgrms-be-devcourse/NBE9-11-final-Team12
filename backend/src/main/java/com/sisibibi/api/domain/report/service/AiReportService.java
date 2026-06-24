package com.sisibibi.api.domain.report.service;

import com.sisibibi.api.domain.report.client.AiReportClient;
import com.sisibibi.api.domain.report.client.dto.AiReportGenerateRes;
import com.sisibibi.api.domain.report.dto.request.AiReportGenerateReq;
import com.sisibibi.api.domain.report.dto.response.AiReportRes;
import com.sisibibi.api.domain.report.prompt.CustomPromptCommand;
import com.sisibibi.api.domain.report.prompt.CustomPromptValidator;
import com.sisibibi.api.global.exception.CustomException;
import com.sisibibi.api.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AiReportService {

    private final AiReportPersistenceService aiReportPersistenceService;
    private final AiReportClient aiReportClient;
    private final CustomPromptValidator customPromptValidator;

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
        AiReportGenerationContext context = userScopedCustomReports
                ? aiReportPersistenceService.prepareGeneration(roomId, userId, customPrompts)
                : aiReportPersistenceService.prepareGeneration(roomId, customPrompts);

        if (!context.shouldCallAi()) {
            return context.response();
        }

        try {
            AiReportGenerateRes response = aiReportClient.generate(context.request());

            if (!isValidResponse(context, response)) {
                if (context.generationType() == AiReportGenerationType.CUSTOM_ONLY) {
                    throw new CustomException(ErrorCode.AI_REPORT_INVALID_RESPONSE);
                }

                return aiReportPersistenceService.fail(
                        context.reportId(),
                        ErrorCode.AI_REPORT_INVALID_RESPONSE.getMessage()
                );
            }

            if (context.generationType() == AiReportGenerationType.CUSTOM_ONLY) {
                if (!userScopedCustomReports) {
                    return aiReportPersistenceService.appendCustomReports(
                            context.reportId(),
                            context.request().customPrompts(),
                            response.customReports()
                    );
                }

                return aiReportPersistenceService.appendCustomReports(
                        context.reportId(),
                        userId,
                        context.request().customPrompts(),
                        response.customReports()
                );
            }

            return aiReportPersistenceService.complete(context.reportId(), response);
        } catch (CustomException e) {
            if (context.generationType() == AiReportGenerationType.CUSTOM_ONLY) {
                throw e;
            }

            return aiReportPersistenceService.fail(context.reportId(), e.getErrorCode().getMessage());
        } catch (RuntimeException e) {
            if (context.generationType() == AiReportGenerationType.CUSTOM_ONLY) {
                throw new CustomException(ErrorCode.AI_REPORT_GENERATE_FAILED);
            }

            return aiReportPersistenceService.fail(
                    context.reportId(),
                    ErrorCode.AI_REPORT_GENERATE_FAILED.getMessage()
            );
        }
    }

    public AiReportRes getReport(Long roomId) {
        return aiReportPersistenceService.getReport(roomId);
    }

    public AiReportRes getReport(Long roomId, Long userId) {
        return aiReportPersistenceService.getReport(roomId, userId);
    }

    private boolean isValidResponse(AiReportGenerationContext context, AiReportGenerateRes response) {
        int customPromptCount = context.request().customPrompts().size();

        return switch (context.generationType()) {
            case BASE_ONLY -> response.hasBaseRequiredFields();
            case BASE_WITH_CUSTOM -> response.hasBaseRequiredFields()
                    && response.hasCustomReports(customPromptCount);
            case CUSTOM_ONLY -> response.hasCustomReports(customPromptCount);
            case SKIP -> true;
        };
    }
}
