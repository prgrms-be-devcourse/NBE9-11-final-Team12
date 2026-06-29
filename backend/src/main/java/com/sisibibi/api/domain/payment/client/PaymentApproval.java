package com.sisibibi.api.domain.payment.client;

public record PaymentApproval(
    String paymentKey,
    String orderId,
    long amount,
    String status
) {
  public PaymentApproval(String paymentKey, String orderId, long amount) {
    this(paymentKey, orderId, amount, "DONE");
  }

  public boolean isDone() {
    return "DONE".equals(status);
  }
}