package com.sisibibi.api.domain.payment.dto.request;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public record TossPaymentWebhookReq(
    String eventType,
    String status,
    String orderId,
    String paymentKey,
    long amount
) {
  public static TossPaymentWebhookReq from(String rawBody, ObjectMapper objectMapper) {
    try {
      JsonNode root = objectMapper.readTree(rawBody);
      JsonNode data = root.path("data");

      long amount = data.hasNonNull("totalAmount")
          ? data.path("totalAmount").asLong()
          : data.path("amount").asLong();

      return new TossPaymentWebhookReq(
          root.path("eventType").asText(null),
          data.path("status").asText(null),
          data.path("orderId").asText(null),
          data.path("paymentKey").asText(null),
          amount
      );
    } catch (Exception exception) {
      throw new IllegalArgumentException("Invalid Toss webhook payload.", exception);
    }
  }

  public boolean isPaymentStatusChanged() {
    return "PAYMENT_STATUS_CHANGED".equals(eventType);
  }

  public boolean isPaymentApproved() {
    return "DONE".equals(status);
  }
}