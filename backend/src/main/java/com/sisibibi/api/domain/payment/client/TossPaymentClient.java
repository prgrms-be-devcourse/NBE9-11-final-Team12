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
    if (!StringUtils.hasText(properties.getSecretKey())) {
      throw new CustomException(ErrorCode.PAYMENT_CONFIRM_FAILED);
    }

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

      if (response == null) {
        throw new CustomException(ErrorCode.PAYMENT_CONFIRM_FAILED);
      }

      return new PaymentApproval(paymentKey, orderId, amount);
    } catch (RuntimeException exception) {
      throw new CustomException(ErrorCode.PAYMENT_CONFIRM_FAILED);
    }
  }

  private String authorizationHeader() {
    String credential = properties.getSecretKey() + ":";
    String encoded = Base64.getEncoder()
        .encodeToString(credential.getBytes(StandardCharsets.UTF_8));
    return "Basic " + encoded;
  }
}