package com.sisibibi.api.domain.payment.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Getter
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(
    name = "payments",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_payments_order_id", columnNames = "order_id"),
        @UniqueConstraint(name = "uk_payments_payment_key", columnNames = "payment_key")
    },
    indexes = {
        @Index(name = "idx_payments_user_created_at", columnList = "user_id, created_at"),
        @Index(name = "idx_payments_target", columnList = "target_type, target_id")
    }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "user_id", nullable = false)
  private Long userId;

  @Column(name = "order_id", nullable = false, length = 100)
  private String orderId;

  @Column(name = "payment_key", length = 200)
  private String paymentKey;

  @Column(nullable = false)
  private long amount;

  @Column(name = "order_name", nullable = false, length = 200)
  private String orderName;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private PaymentStatus status;

  @Enumerated(EnumType.STRING)
  @Column(name = "target_type", nullable = false, length = 50)
  private PaymentTargetType targetType;

  @Column(name = "target_id", nullable = false)
  private Long targetId;

  @Column(name = "approved_at")
  private LocalDateTime approvedAt;

  @Column(name = "failed_at")
  private LocalDateTime failedAt;

  @Column(name = "failure_reason", length = 1000)
  private String failureReason;

  @CreatedDate
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @LastModifiedDate
  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  public static Payment pending(
      Long userId,
      String orderId,
      long amount,
      String orderName,
      PaymentTargetType targetType,
      Long targetId
  ) {
    Payment payment = new Payment();
    payment.userId = userId;
    payment.orderId = orderId;
    payment.amount = amount;
    payment.orderName = orderName;
    payment.targetType = targetType;
    payment.targetId = targetId;
    payment.status = PaymentStatus.PENDING;
    return payment;
  }

  public void complete(String paymentKey) {
    if (status == PaymentStatus.COMPLETED) {
      return;
    }
    if (status == PaymentStatus.CANCELED) {
      throw new IllegalStateException("Canceled payment cannot be completed.");
    }

    this.paymentKey = paymentKey;
    this.status = PaymentStatus.COMPLETED;
    this.approvedAt = LocalDateTime.now();
    this.failureReason = null;
  }

  public void fail(String reason) {
    if (status == PaymentStatus.COMPLETED) {
      return;
    }

    this.status = PaymentStatus.FAILED;
    this.failedAt = LocalDateTime.now();
    this.failureReason = reason;
  }
}