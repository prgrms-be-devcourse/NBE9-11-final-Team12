package com.sisibibi.api.domain.payment.client;

import com.sisibibi.api.global.exception.CustomException;
import com.sisibibi.api.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

@Component
@RequiredArgsConstructor
public class TossWebhookVerifier {

  private static final String HMAC_SHA256 = "HmacSHA256";

  private final TossPaymentProperties properties;

  public void verify(String rawBody, String signature) {
    if (!StringUtils.hasText(properties.getWebhookSecret())) {
      throw new CustomException(ErrorCode.PAYMENT_CONFIRM_FAILED);
    }

    if (!StringUtils.hasText(signature)) {
      throw new CustomException(ErrorCode.UNAUTHORIZED);
    }

    String expected = hmacSha256(rawBody, properties.getWebhookSecret());
    if (!MessageDigest.isEqual(
        expected.getBytes(StandardCharsets.UTF_8),
        signature.getBytes(StandardCharsets.UTF_8)
    )) {
      throw new CustomException(ErrorCode.UNAUTHORIZED);
    }
  }

  private String hmacSha256(String rawBody, String secret) {
    try {
      Mac mac = Mac.getInstance(HMAC_SHA256);
      mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256));
      return HexFormat.of().formatHex(mac.doFinal(rawBody.getBytes(StandardCharsets.UTF_8)));
    } catch (Exception exception) {
      throw new CustomException(ErrorCode.PAYMENT_CONFIRM_FAILED);
    }
  }
}