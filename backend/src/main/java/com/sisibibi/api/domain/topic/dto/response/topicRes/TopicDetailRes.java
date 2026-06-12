package com.sisibibi.api.domain.topic.dto.response.topicRes;

import com.sisibibi.api.domain.topic.entity.Topic;

import java.time.LocalDateTime;

public record TopicDetailRes(
    Long id,
    String title,
    String description,
    String category,
    String sourceUrl,
    LocalDateTime createdAt,
    LocalDateTime approvedAt
) {

  public static TopicDetailRes from(Topic topic) {
    return new TopicDetailRes(
        topic.getId(),
        topic.getTitle(),
        topic.getDescription(),
        topic.getCategory(),
        topic.getSourceUrl(),
        topic.getCreatedAt(),
        topic.getApprovedAt()
    );
  }
}