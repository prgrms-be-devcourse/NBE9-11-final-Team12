package com.sisibibi.api.domain.payment.client;

public record PaymentApproval(
    String paymentKey,
    String orderId,
    long amount
) {
}