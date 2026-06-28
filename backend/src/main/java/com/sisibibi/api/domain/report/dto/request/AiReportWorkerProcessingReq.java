package com.sisibibi.api.domain.report.dto.request;

import com.sisibibi.api.domain.report.service.AiReportGenerationType;
import jakarta.validation.constraints.NotNull;

public record AiReportWorkerProcessingReq(
        @NotNull AiReportGenerationType generationType
) {
}
