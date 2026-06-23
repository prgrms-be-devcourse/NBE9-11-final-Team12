package com.sisibibi.api.domain.report.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.util.StringUtils;

import java.util.List;

public record AiReportGenerateRes(
        @JsonProperty("핵심 한줄")
        String coreLine,

        @JsonProperty("핵심 쟁점")
        List<String> keyIssues,

        @JsonProperty("AI 종합 정리")
        String aiSummary,

        @JsonProperty("공통 의견")
        String commonGround,

        @JsonProperty("AI의 개인적 소견")
        String aiOpinion,

        List<AiReportCustomReportPayload> customReports
) {

    public AiReportGenerateRes(
            String coreLine,
            List<String> keyIssues,
            String aiSummary,
            String commonGround,
            String aiOpinion
    ) {
        this(coreLine, keyIssues, aiSummary, commonGround, aiOpinion, List.of());
    }

    public AiReportGenerateRes {
        customReports = customReports == null ? List.of() : List.copyOf(customReports);
    }

    public boolean hasRequiredFields() {
        return hasBaseRequiredFields();
    }

    public boolean hasBaseRequiredFields() {
        return StringUtils.hasText(coreLine)
                && keyIssues != null
                && StringUtils.hasText(aiSummary)
                && StringUtils.hasText(commonGround)
                && StringUtils.hasText(aiOpinion);
    }

    public boolean hasCustomReports(int expectedCount) {
        return customReports != null
                && customReports.size() == expectedCount
                && customReports.stream()
                .allMatch(report -> StringUtils.hasText(report.label())
                        && StringUtils.hasText(report.content()));
    }
}
