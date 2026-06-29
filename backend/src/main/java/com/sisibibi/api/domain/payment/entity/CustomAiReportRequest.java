package com.sisibibi.api.domain.payment.entity;

import com.sisibibi.api.domain.report.entity.AiReportCustomPrompt;
import com.sisibibi.api.domain.report.entity.CustomPromptsConverter;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(
    name = "custom_ai_report_requests",
    indexes = {
        @Index(name = "idx_custom_ai_report_requests_user_created_at", columnList = "user_id, created_at"),
        @Index(name = "idx_custom_ai_report_requests_room", columnList = "room_id")
    }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CustomAiReportRequest {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "room_id", nullable = false)
  private Long roomId;

  @Column(name = "user_id", nullable = false)
  private Long userId;

  @Column(name = "payment_id")
  private Long paymentId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private CustomAiReportRequestStatus status;

  @Convert(converter = CustomPromptsConverter.class)
  @Column(name = "custom_prompts", nullable = false, columnDefinition = "TEXT")
  private List<AiReportCustomPrompt> customPrompts = List.of();

  @Column(name = "last_error_message", length = 1000)
  private String lastErrorMessage;

  @Column(name = "paid_at")
  private LocalDateTime paidAt;

  @Column(name = "queued_at")
  private LocalDateTime queuedAt;

  @CreatedDate
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @LastModifiedDate
  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  public static CustomAiReportRequest pendingPayment(
      Long roomId,
      Long userId,
      List<AiReportCustomPrompt> customPrompts
  ) {
    CustomAiReportRequest request = new CustomAiReportRequest();
    request.roomId = roomId;
    request.userId = userId;
    request.status = CustomAiReportRequestStatus.PENDING_PAYMENT;
    request.customPrompts = customPrompts == null ? List.of() : List.copyOf(customPrompts);
    return request;
  }

  public void linkPayment(Long paymentId) {
    this.paymentId = paymentId;
  }

  public void markPaid() {
    if (status == CustomAiReportRequestStatus.QUEUED) {
      return;
    }

    this.status = CustomAiReportRequestStatus.PAID;
    this.paidAt = LocalDateTime.now();
    this.lastErrorMessage = null;
  }

  public void markQueued() {
    this.status = CustomAiReportRequestStatus.QUEUED;
    this.queuedAt = LocalDateTime.now();
    this.lastErrorMessage = null;
  }

  public void markPublishFailed(String message) {
    this.status = CustomAiReportRequestStatus.PUBLISH_FAILED;
    this.lastErrorMessage = message;
  }
}