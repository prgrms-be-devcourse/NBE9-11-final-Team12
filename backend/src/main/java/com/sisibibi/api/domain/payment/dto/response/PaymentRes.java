package com.sisibibi.api.domain.payment.dto.response;

import com.sisibibi.api.domain.payment.entity.Payment;

import java.time.LocalDateTime;

public record PaymentRes(
    Long paymentId,
    String orderId,
    String paymentKey,
    long amount,
    String orderName,
    String status,
    String targetType,
    Long targetId,
    LocalDateTime approvedAt,
    LocalDateTime createdAt
) {
  public static PaymentRes from(Payment payment) {
    return new PaymentRes(
        payment.getId(),
        payment.getOrderId(),
        payment.getPaymentKey(),
        payment.getAmount(),
        payment.getOrderName(),
        payment.getStatus().name(),
        payment.getTargetType().name(),
        payment.getTargetId(),
        payment.getApprovedAt(),
        payment.getCreatedAt()
    );
  }
}