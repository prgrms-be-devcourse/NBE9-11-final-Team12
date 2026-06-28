package com.sisibibi.api.domain.report.entity;

public enum AiReportStatus {
    REQUESTED,
    QUEUED,
    PROCESSING,
    COMPLETED,
    PUBLISH_FAILED,
    GENERATION_FAILED,
    BLOCKED
}
