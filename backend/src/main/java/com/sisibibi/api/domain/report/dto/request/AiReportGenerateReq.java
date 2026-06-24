package com.sisibibi.api.domain.report.dto.request;

import java.util.List;

public record AiReportGenerateReq(
        List<CustomPromptReq> customPrompts
) {

    public AiReportGenerateReq {
        customPrompts = customPrompts == null ? List.of() : List.copyOf(customPrompts);
    }

    public static AiReportGenerateReq empty() {
        return new AiReportGenerateReq(List.of());
    }

    public record CustomPromptReq(
            String label,
            String prompt
    ) {
    }
}
