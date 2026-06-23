package com.sisibibi.api.domain.report.entity;

public record AiReportCustomReport(
        String requestLabel,
        String prompt,
        String resultLabel,
        String content
) {
}
