package com.sisibibi.api.domain.topic.dto.response.topicRes;

import com.sisibibi.api.domain.topic.entity.Topic;
import com.sisibibi.api.domain.topic.entity.TopicStatus;

public record TopicCreateRes(
    Long topicId,
    TopicStatus status
) {

  public static TopicCreateRes from(Topic topic) {
    return new TopicCreateRes(
        topic.getId(),
        topic.getStatus()
    );
  }
}
