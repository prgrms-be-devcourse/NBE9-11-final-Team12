package com.sisibibi.api.domain.payment.client;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.payment.toss")
public class TossPaymentProperties {

  private String baseUrl = "https://api.tosspayments.com";
  private String secretKey;

}