package com.sisibibi.api.domain.report.prompt;

public enum PromptSeverity {
    SAFE,
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL,
    UNKNOWN;

    public boolean blocksRequest() {
        return this == HIGH || this == CRITICAL;
    }

    public boolean requiresAuditLog() {
        return this == MEDIUM;
    }

    public int rank() {
        return switch (this) {
            case SAFE -> 0;
            case LOW -> 1;
            case MEDIUM -> 2;
            case HIGH -> 3;
            case CRITICAL -> 4;
            case UNKNOWN -> -1;
        };
    }

    public static PromptSeverity from(String value) {
        if (value == null || value.isBlank()) {
            return UNKNOWN;
        }

        try {
            return PromptSeverity.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return UNKNOWN;
        }
    }
}
