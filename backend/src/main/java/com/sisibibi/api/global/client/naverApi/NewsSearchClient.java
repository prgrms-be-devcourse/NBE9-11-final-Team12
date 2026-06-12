package com.sisibibi.api.global.client.naverApi;

import com.fasterxml.jackson.databind.JsonNode;
import com.sisibibi.api.domain.topic.dto.request.NewsSearchCommand;
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
public class NewsSearchClient implements NewsClient {

  private final RestClient restClient;
  private final SearchProperties properties;

  public NewsSearchClient(RestClient.Builder restClientBuilder, SearchProperties properties) {
    this.restClient = restClientBuilder
        .baseUrl(properties.getNewsUrl())
        .build();
    this.properties = properties;
  }

  @Override
  public JsonNode search(NewsSearchCommand command) {
    validateCredentials();

    try {
      return restClient.get()
          .uri(uriBuilder -> uriBuilder
              .queryParam("query", command.query())
              .queryParam("display", command.display())
              .queryParam("start", command.start())
              .queryParam("sort", command.sort())
              .build())
          .header("X-Naver-Client-Id", properties.getClientId())
          .header("X-Naver-Client-Secret", properties.getClientSecret())
          .retrieve()
          .body(JsonNode.class);
    } catch (RestClientResponseException e) {
      log.warn("Naver news search failed. status={}, response={}",
          e.getStatusCode(),
          e.getResponseBodyAsString()
      );
      throw new CustomException(ErrorCode.NAVER_SEARCH_FAILED);
    } catch (RestClientException e) {
      log.warn("Naver news search request failed.", e);
      throw new CustomException(ErrorCode.NAVER_SEARCH_FAILED);
    }
  }

  private void validateCredentials() {
    if (!StringUtils.hasText(properties.getClientId())
        || !StringUtils.hasText(properties.getClientSecret())) {
      throw new CustomException(ErrorCode.NAVER_SEARCH_CONFIG_MISSING);
    }
  }
}