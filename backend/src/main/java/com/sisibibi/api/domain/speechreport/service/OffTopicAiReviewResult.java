package com.sisibibi.api.domain.speechreport.service;

public record OffTopicAiReviewResult(
        boolean offTopic,
        String reason,
        double confidence
) {
}
