package com.sisibibi.api.global.client.serpApi;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "serpapi")
public class SerpApiProperties {

  private String baseUrl = "https://serpapi.com/search.json";
  private String apiKey;
}