package com.sisibibi.api.global.client.serpApi;

import com.fasterxml.jackson.databind.JsonNode;
import com.sisibibi.api.global.exception.CustomException;
import com.sisibibi.api.global.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Slf4j
@Component
public class SerpApiGoogleTrendsClient implements GoogleTrendsClient {

  private static final String ENGINE = "google_trends_trending_now";
  private static final String KOREA_GEO = "KR";
  private static final String KOREAN_LANGUAGE = "ko";
  private static final int PAST_24_HOURS = 24;

  private final RestClient restClient;
  private final SerpApiProperties properties;

  public SerpApiGoogleTrendsClient(RestClient.Builder restClientBuilder, SerpApiProperties properties) {
    this.restClient = restClientBuilder
        .baseUrl(properties.getBaseUrl())
        .build();
    this.properties = properties;
  }

  @Override
  public JsonNode getKoreaTrendingNow() {
    validateApiKey();

    try {
      return restClient.get()
          .uri(uriBuilder -> uriBuilder
              .queryParam("engine", ENGINE)
              .queryParam("geo", KOREA_GEO)
              .queryParam("hl", KOREAN_LANGUAGE)
              .queryParam("hours", PAST_24_HOURS)
              .queryParam("only_active", true)
              .queryParam("api_key", properties.getApiKey())
              .build())
          .retrieve()
          .body(JsonNode.class);
    } catch (RestClientResponseException e) {
      log.warn("SerpApi Google Trends request failed. status={}, response={}",
          e.getStatusCode(),
          e.getResponseBodyAsString()
      );
      throw new CustomException(ErrorCode.SERPAPI_GOOGLE_TRENDS_FAILED);
    } catch (RestClientException e) {
      log.warn("SerpApi Google Trends request failed.", e);
      throw new CustomException(ErrorCode.SERPAPI_GOOGLE_TRENDS_FAILED);
    }
  }

  private void validateApiKey() {
    if (!StringUtils.hasText(properties.getApiKey())) {
      throw new CustomException(ErrorCode.SERPAPI_CONFIG_MISSING);
    }
  }
}
