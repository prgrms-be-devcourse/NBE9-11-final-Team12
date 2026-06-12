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

  private String title;

  private String description;

  private String category;

  private String sourceUrl;

  @Enumerated(EnumType.STRING)
  private TopicStatus status;

  private LocalDateTime createdAt;

  private LocalDateTime approvedAt;

  private Long approvedBy;
}
