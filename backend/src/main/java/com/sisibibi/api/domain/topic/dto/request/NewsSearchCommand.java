package com.sisibibi.api.domain.topic.dto.request;

public record NewsSearchCommand(
    String query,
    int display,
    int start,
    String sort
) {
}