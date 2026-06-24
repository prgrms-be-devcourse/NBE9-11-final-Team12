package com.sisibibi.api.domain.report.service;

import com.sisibibi.api.domain.report.client.dto.AiReportGenerateReq;
import com.sisibibi.api.domain.report.dto.response.AiReportRes;

public record AiReportGenerationContext(
        boolean shouldCallAi,
        AiReportGenerationType generationType,
        Long reportId,
        AiReportGenerateReq request,
        AiReportRes response
) {

    public static AiReportGenerationContext callAi(Long reportId, AiReportGenerateReq request) {
        return callAi(reportId, request, AiReportGenerationType.BASE_ONLY);
    }

    public static AiReportGenerationContext callAi(
            Long reportId,
            AiReportGenerateReq request,
            AiReportGenerationType generationType
    ) {
        return new AiReportGenerationContext(true, generationType, reportId, request, null);
    }

    public static AiReportGenerationContext skipAi(AiReportRes response) {
        return new AiReportGenerationContext(false, AiReportGenerationType.SKIP, response.reportId(), null, response);
    }
}
