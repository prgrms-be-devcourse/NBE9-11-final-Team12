package com.sisibibi.api.domain.topic.dto.response;

import java.util.List;

public record NewsSearchRes(
    String lastBuildDate,
    int total,
    int start,
    int display,
    List<IssueNewsRes> items
) {
}