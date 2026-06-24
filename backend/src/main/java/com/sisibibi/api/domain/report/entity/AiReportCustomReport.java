package com.sisibibi.api.domain.report.entity;

import java.util.Objects;

public record AiReportCustomReport(
        Long userId,
        String requestLabel,
        String prompt,
        String resultLabel,
        String content
) {
    public AiReportCustomReport(String requestLabel, String prompt, String resultLabel, String content) {
        this(null, requestLabel, prompt, resultLabel, content);
    }

    public boolean isVisibleTo(Long viewerUserId) {
        return viewerUserId == null || Objects.equals(userId, viewerUserId);
    }
}
