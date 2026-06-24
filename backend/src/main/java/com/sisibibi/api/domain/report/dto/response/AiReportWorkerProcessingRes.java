package com.sisibibi.api.domain.report.dto.response;

import com.sisibibi.api.domain.report.client.dto.AiReportGenerateReq;
import com.sisibibi.api.domain.report.service.AiReportGenerationType;

public record AiReportWorkerProcessingRes(
        Long reportId,
        Long roomId,
        AiReportGenerationType generationType,
        AiReportGenerateReq request
) {
}
