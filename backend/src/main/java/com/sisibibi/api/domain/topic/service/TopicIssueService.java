package com.sisibibi.api.domain.topic.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.sisibibi.api.global.client.serpApi.GoogleTrendsClient;
import com.sisibibi.api.global.client.naverApi.NewsClient;
import com.sisibibi.api.domain.topic.dto.request.NewsSearchCommand;
import com.sisibibi.api.domain.topic.dto.response.IssueCandidateRes;
import com.sisibibi.api.domain.topic.dto.response.IssueNewsRes;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TopicIssueService {

  private static final int TREND_KEYWORD_LIMIT = 10;
  private static final int NEWS_PER_KEYWORD = 3;
  private static final int NAVER_NEWS_START = 1;
  private static final String NAVER_NEWS_SORT_DATE = "date";

  private final GoogleTrendsClient googleTrendsClient;
  private final NewsClient naverNewsClient;

  public List<IssueCandidateRes> createKoreaIssueCandidates() {
    JsonNode trends = googleTrendsClient.getKoreaTrendingNow().path("trending_searches");

    List<IssueCandidateRes> result = new ArrayList<>();

    for (JsonNode trend : trends) {
      if (result.size() >= TREND_KEYWORD_LIMIT) {
        break;
      }

      String keyword = trend.path("query").asText();

      if (keyword.isBlank()) {
        continue;
      }

      List<IssueNewsRes> news = searchRecentNews(keyword);

      if (news.size() < NEWS_PER_KEYWORD) {
        continue;
      }

      result.add(new IssueCandidateRes(
          keyword,
          trend.path("search_volume").asLong(),
          trend.path("increase_percentage").asInt(),
          news
      ));
    }

    return result;
  }

  private List<IssueNewsRes> searchRecentNews(String keyword) {
    JsonNode items = naverNewsClient.search(
        new NewsSearchCommand(
            keyword,
            NEWS_PER_KEYWORD,
            NAVER_NEWS_START,
            NAVER_NEWS_SORT_DATE
        )
    ).path("items");

    List<IssueNewsRes> news = new ArrayList<>();

    for (JsonNode item : items) {
      news.add(IssueNewsRes.from(item));
    }

    return news;
  }
}