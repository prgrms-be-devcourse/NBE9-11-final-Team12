package com.sisibibi.api.domain.topic.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "topic.ai.classification")
public record TopicAiClassificationProperties(
    Mode mode
) {

  public TopicAiClassificationProperties {
    if (mode == null) {
      mode = Mode.PARALLEL;
    }
  }

  public enum Mode {
    SEQUENTIAL,
    PARALLEL
  }
}