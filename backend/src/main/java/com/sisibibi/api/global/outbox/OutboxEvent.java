package com.sisibibi.api.global.outbox;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
    name = "outbox_events",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_outbox_events_event_id",
            columnNames = "event_id"
        ),
        @UniqueConstraint(
            name = "uk_outbox_events_deduplication_key",
            columnNames = "deduplication_key"
        )
    },
    indexes = {
        @Index(
            name = "idx_outbox_events_status_next_retry",
            columnList = "status, next_retry_at"
        ),
        @Index(
            name = "idx_outbox_events_aggregate",
            columnList = "aggregate_type, aggregate_id"
        )
    }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OutboxEvent {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "event_id", nullable = false, length = 36)
  private String eventId;

  @Column(name = "aggregate_type", nullable = false, length = 50)
  private String aggregateType;

  @Column(name = "aggregate_id", nullable = false)
  private Long aggregateId;

  @Enumerated(EnumType.STRING)
  @Column(name = "event_type", nullable = false, length = 100)
  private OutboxEventType eventType;

  @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
  private String payload;

  @Column(name = "deduplication_key", nullable = false, length = 150)
  private String deduplicationKey;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 20)
  private OutboxEventStatus status;

  @Column(name = "retry_count", nullable = false)
  private int retryCount;

  @Column(name = "next_retry_at")
  private LocalDateTime nextRetryAt;

  @Column(name = "last_error_message", columnDefinition = "TEXT")
  private String lastErrorMessage;

  @Column(name = "occurred_at", nullable = false)
  private LocalDateTime occurredAt;

  @Column(name = "published_at")
  private LocalDateTime publishedAt;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  private OutboxEvent(
      String aggregateType,
      Long aggregateId,
      OutboxEventType eventType,
      String payload,
      String deduplicationKey,
      LocalDateTime occurredAt
  ) {
    LocalDateTime now = LocalDateTime.now();

    this.eventId = UUID.randomUUID().toString();
    this.aggregateType = aggregateType;
    this.aggregateId = aggregateId;
    this.eventType = eventType;
    this.payload = payload;
    this.deduplicationKey = deduplicationKey;
    this.status = OutboxEventStatus.PENDING;
    this.retryCount = 0;
    this.nextRetryAt = now;
    this.occurredAt = occurredAt;
    this.createdAt = now;
    this.updatedAt = now;
  }

  public static OutboxEvent pending(
      String aggregateType,
      Long aggregateId,
      OutboxEventType eventType,
      String payload,
      String deduplicationKey,
      LocalDateTime occurredAt
  ) {
    return new OutboxEvent(
        aggregateType,
        aggregateId,
        eventType,
        payload,
        deduplicationKey,
        occurredAt
    );
  }

  public void markPublishing(LocalDateTime now) {
    this.status = OutboxEventStatus.PUBLISHING;
    this.updatedAt = now;
  }

  public void markSent(LocalDateTime now) {
    this.status = OutboxEventStatus.SENT;
    this.publishedAt = now;
    this.updatedAt = now;
  }

  public void markRetry(String errorMessage, LocalDateTime nextRetryAt, LocalDateTime now) {
    this.status = OutboxEventStatus.PENDING;
    this.retryCount += 1;
    this.nextRetryAt = nextRetryAt;
    this.lastErrorMessage = errorMessage;
    this.updatedAt = now;
  }

  public void markFailed(String errorMessage, LocalDateTime now) {
    this.status = OutboxEventStatus.FAILED;
    this.lastErrorMessage = errorMessage;
    this.updatedAt = now;
  }
}