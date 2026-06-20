package com.sisibibi.api.domain.topic.dto.response.keywordres;

import com.sisibibi.api.domain.topic.dto.response.issueRes.IssueNewsRes;

import java.util.List;

public record ClassifiedIssueNewsRes(
    IssueNewsRes news,
    String category,
    List<String> keywords
) {
}
