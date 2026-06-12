package com.sisibibi.api.global.client.naverApi;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "naver.search")
public class SearchProperties {

  private String newsUrl = "https://openapi.naver.com/v1/search/news.json";
  private String clientId;
  private String clientSecret;
}
