package com.sisibibi.api.domain.report.service;

import com.sisibibi.api.domain.report.dto.response.AiReportRes;

public record AiReportRequestResult(
        AiReportRes response,
        boolean shouldPublish,
        AiReportGenerationType generationType
) {

    public static AiReportRequestResult publish(AiReportRes response, AiReportGenerationType generationType) {
        return new AiReportRequestResult(response, true, generationType);
    }

    public static AiReportRequestResult skip(AiReportRes response) {
        return new AiReportRequestResult(response, false, AiReportGenerationType.SKIP);
    }
}
