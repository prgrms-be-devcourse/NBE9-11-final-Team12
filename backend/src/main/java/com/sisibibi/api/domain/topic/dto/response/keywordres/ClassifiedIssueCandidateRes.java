package com.sisibibi.api.domain.topic.dto.response.keywordres;

import java.util.List;

public record ClassifiedIssueCandidateRes(
    String keyword,
    Long searchVolume,
    Integer increasePercentage,
    List<ClassifiedIssueNewsRes> news
) {
}