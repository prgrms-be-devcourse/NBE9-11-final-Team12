package com.sisibibi.api.domain.topic.dto.request;


public record CreateTopicReq(
    String title,

    String description,

    String category,

    String sourceUrl
) {
}