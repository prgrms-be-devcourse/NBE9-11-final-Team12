package com.sisibibi.api.domain.topic.dto.response;

import java.util.List;

public record IssueCandidateRes(
    String keyword,
    long searchVolume,
    int increasePercentage,
    List<IssueNewsRes> news
) {
}