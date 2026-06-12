package com.sisibibi.api.domain.topic.dto.response.topicRes;

import com.sisibibi.api.domain.topic.entity.Topic;

import java.time.LocalDateTime;

public record TopicSummaryRes(
    Long id,
    String title,
    String category,
    String sourceUrl,
    LocalDateTime createdAt,
    LocalDateTime approvedAt
) {

  public static TopicSummaryRes from(Topic topic) {
    return new TopicSummaryRes(
        topic.getId(),
        topic.getTitle(),
        topic.getCategory(),
        topic.getSourceUrl(),
        topic.getCreatedAt(),
        topic.getApprovedAt()
    );
  }
}