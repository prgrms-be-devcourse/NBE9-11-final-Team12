package com.sisibibi.api.domain.report.entity;

public record AiReportCustomPrompt(
        Long userId,
        String label,
        String prompt
) {
    public AiReportCustomPrompt(String label, String prompt) {
        this(null, label, prompt);
    }
}
