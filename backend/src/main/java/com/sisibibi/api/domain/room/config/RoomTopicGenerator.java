package com.sisibibi.api.domain.room.config;

import com.sisibibi.api.domain.topic.entity.Topic;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class RoomTopicGenerator {

  private final ChatClient chatClient;

  public RoomTopicGenerator(ChatClient.Builder chatClientBuilder) {
    this.chatClient = chatClientBuilder.build();
  }

  public String generate(Topic topic) {
    try {
      String result = chatClient.prompt()
          .system("""
              당신은 토론방 제목을 만드는 도우미입니다.
              승인된 토픽을 바탕으로 찬반 토론이 가능한 제목을 한 줄로 만드세요.
              조건:
              - 한국어로 작성
              - 20자 이상 60자 이하
              - 설명 없이 제목만 반환
              - 따옴표를 붙이지 않음
              - 중립적이고 자극적이지 않게 작성
              """)
          .user(user -> user.text("""
              원본 토픽 제목: {title}
              설명: {description}
              카테고리: {category}
              """)
              .param("title", topic.getTitle())
              .param("description", topic.getDescription())
              .param("category", topic.getCategory()))
          .call()
          .content();

      return normalize(result, topic.getTitle());
    } catch (Exception e) {
      log.warn("Failed to generate debate room title. topicId={}", topic.getId(), e);
      return topic.getTitle();
    }
  }

  private String normalize(String value, String fallback) {
    if (value == null || value.isBlank()) {
      return fallback;
    }

    String normalized = value
        .replace("\"", "")
        .replace("'", "")
        .trim();

    if (normalized.isBlank()) {
      return fallback;
    }

    if (normalized.length() > 100) {
      return normalized.substring(0, 100);
    }

    return normalized;
  }
}
