package com.sisibibi.api.domain.report.service;

import com.sisibibi.api.domain.report.client.dto.AiReportGenerateReq;
import com.sisibibi.api.domain.report.dto.response.AiReportRes;

public record AiReportGenerationContext(
        boolean shouldCallAi,
        Long reportId,
        AiReportGenerateReq request,
        AiReportRes response
) {

    public static AiReportGenerationContext callAi(Long reportId, AiReportGenerateReq request) {
        return new AiReportGenerationContext(true, reportId, request, null);
    }

    public static AiReportGenerationContext skipAi(AiReportRes response) {
        return new AiReportGenerationContext(false, response.reportId(), null, response);
    }
}
