package com.sisibibi.api.domain.report.entity;

public enum AiReportStatus {
    REQUESTED,
    QUEUED,
    PROCESSING,
    COMPLETED,
    PUBLISH_FAILED,
    GENERATION_FAILED,
    BLOCKED,

    // Legacy alias kept temporarily until tests and API clients move to the async status model.
    PENDING,

    // Legacy alias kept temporarily until tests and API clients move to the async status model.
    FAILED
}
