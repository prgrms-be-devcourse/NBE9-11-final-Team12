package com.sisibibi.api.domain.report.client.dto;

import java.util.List;

public record AiReportBaseReportPayload(
        String coreLine,
        List<String> keyIssues,
        String aiSummary,
        String commonGround,
        String aiOpinion
) {

    public AiReportBaseReportPayload {
        keyIssues = keyIssues == null ? List.of() : List.copyOf(keyIssues);
    }
}
