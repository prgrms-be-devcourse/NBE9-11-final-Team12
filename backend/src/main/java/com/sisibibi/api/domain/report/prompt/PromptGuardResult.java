package com.sisibibi.api.domain.report.prompt;

public record PromptGuardResult(
        boolean blocked,
        PromptSeverity severity,
        String reason
) {

    public PromptGuardResult {
        severity = severity == null ? PromptSeverity.UNKNOWN : severity;
    }

    public static PromptGuardResult allowed(PromptSeverity severity, String reason) {
        return new PromptGuardResult(false, severity, reason);
    }

    public static PromptGuardResult blocked(PromptSeverity severity, String reason) {
        return new PromptGuardResult(true, severity, reason);
    }
}
