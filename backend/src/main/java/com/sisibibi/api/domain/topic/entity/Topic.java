package com.sisibibi.api.domain.topic.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "topics")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Topic {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String title;

  private String description;

  private String category;

  private String sourceUrl;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private TopicStatus status;

  @Column(nullable = false)
  private LocalDateTime createdAt;

  private LocalDateTime approvedAt;

  private Long approvedBy;

  public static Topic approved(
      String title,
      String description,
      String category,
      String sourceUrl
  ) {
    LocalDateTime now = LocalDateTime.now();
    Topic topic = new Topic();
    topic.title = title;
    topic.description = description;
    topic.category = category;
    topic.sourceUrl = sourceUrl;
    topic.status = TopicStatus.APPROVED;
    topic.createdAt = now;
    topic.approvedAt = now;
    return topic;
  }

  public void update(
      String title,
      String description,
      String category,
      String sourceUrl
  ) {
    this.title = title;
    this.description = description;
    this.category = category;
    this.sourceUrl = sourceUrl;
  }

}
