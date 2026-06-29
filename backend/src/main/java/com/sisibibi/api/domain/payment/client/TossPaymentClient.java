package com.sisibibi.api.domain.payment.client;

import com.sisibibi.api.global.exception.CustomException;
import com.sisibibi.api.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class TossPaymentClient implements PaymentClient {

  private final RestClient.Builder restClientBuilder;
  private final TossPaymentProperties properties;

  @Override
  public PaymentApproval confirm(String paymentKey, String orderId, long amount) {
    validateSecretKey();

    try {
      Map<?, ?> response = restClientBuilder
          .baseUrl(properties.getBaseUrl())
          .build()
          .post()
          .uri("/v1/payments/confirm")
          .header("Authorization", authorizationHeader())
          .body(Map.of(
              "paymentKey", paymentKey,
              "orderId", orderId,
              "amount", amount
          ))
          .retrieve()
          .body(Map.class);

      return toApproval(response);
    } catch (RuntimeException exception) {
      throw new CustomException(ErrorCode.PAYMENT_CONFIRM_FAILED);
    }
  }

  @Override
  public PaymentApproval getPayment(String paymentKey) {
    validateSecretKey();

    try {
      Map<?, ?> response = restClientBuilder
          .baseUrl(properties.getBaseUrl())
          .build()
          .get()
          .uri("/v1/payments/{paymentKey}", paymentKey)
          .header("Authorization", authorizationHeader())
          .retrieve()
          .body(Map.class);

      return toApproval(response);
    } catch (RuntimeException exception) {
      throw new CustomException(ErrorCode.PAYMENT_CONFIRM_FAILED);
    }
  }

  private void validateSecretKey() {
    if (!StringUtils.hasText(properties.getSecretKey())) {
      throw new CustomException(ErrorCode.PAYMENT_CONFIRM_FAILED);
    }
  }

  private PaymentApproval toApproval(Map<?, ?> response) {
    if (response == null) {
      throw new CustomException(ErrorCode.PAYMENT_CONFIRM_FAILED);
    }

    String paymentKey = stringValue(response.get("paymentKey"));
    String orderId = stringValue(response.get("orderId"));
    String status = stringValue(response.get("status"));
    long amount = longValue(response.get("totalAmount"));

    if (!StringUtils.hasText(paymentKey)
        || !StringUtils.hasText(orderId)
        || !StringUtils.hasText(status)
        || amount <= 0) {
      throw new CustomException(ErrorCode.PAYMENT_CONFIRM_FAILED);
    }

    return new PaymentApproval(paymentKey, orderId, amount, status);
  }

  private String stringValue(Object value) {
    return value == null ? null : String.valueOf(value);
  }

  private long longValue(Object value) {
    if (value instanceof Number number) {
      return number.longValue();
    }

    if (value == null) {
      return 0L;
    }

    try {
      return Long.parseLong(String.valueOf(value));
    } catch (NumberFormatException exception) {
      return 0L;
    }
  }

  private String authorizationHeader() {
    String credential = properties.getSecretKey() + ":";
    String encoded = Base64.getEncoder()
        .encodeToString(credential.getBytes(StandardCharsets.UTF_8));
    return "Basic " + encoded;
  }
}