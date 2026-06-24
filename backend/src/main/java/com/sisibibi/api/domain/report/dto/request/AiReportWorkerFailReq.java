package com.sisibibi.api.domain.report.dto.request;

import jakarta.validation.constraints.NotBlank;

public record AiReportWorkerFailReq(
        String errorCode,
        @NotBlank String errorMessage
) {
}
