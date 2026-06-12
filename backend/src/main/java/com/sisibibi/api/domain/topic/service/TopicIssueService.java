package com.sisibibi.api.domain.topic.service;

import com.sisibibi.api.domain.topic.dto.response.issueRes.NewsSearchRes;
import com.sisibibi.api.domain.topic.dto.response.issueRes.TrendingSearchRes;
import com.sisibibi.api.global.client.serpApi.GoogleTrendsClient;
import com.sisibibi.api.global.client.naverApi.NewsClient;
import com.sisibibi.api.domain.topic.dto.request.NewsSearchCommand;
import com.sisibibi.api.domain.topic.dto.response.issueRes.IssueCandidateRes;
import com.sisibibi.api.domain.topic.dto.response.issueRes.IssueNewsRes;
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

  public List<IssueCandidateRes> createIssue() {
    List<TrendingSearchRes> trends = googleTrendsClient.getTrendingNow().trendingSearches();

    List<IssueCandidateRes> result = new ArrayList<>();

    if (trends == null) {
      return result;
    }

    for (TrendingSearchRes trend : trends) {
      if (result.size() >= TREND_KEYWORD_LIMIT) {
        break;
      }

      String keyword = trend.query();

      if (keyword == null || keyword.isBlank()) {
        continue;
      }

      List<IssueNewsRes> news = searchRecentNews(keyword);

      if (news.size() < NEWS_PER_KEYWORD) {
        continue;
      }

      result.add(new IssueCandidateRes(
          keyword,
          trend.searchVolume(),
          trend.increasePercentage(),
          news
      ));
    }

    return result;
  }

  private List<IssueNewsRes> searchRecentNews(String keyword) {
    NewsSearchRes response = naverNewsClient.search(
        new NewsSearchCommand(
            keyword,
            NEWS_PER_KEYWORD,
            NAVER_NEWS_START,
            NAVER_NEWS_SORT_DATE
        )
    );

    if (response.items() == null) {
      return new ArrayList<>();
    }

    return response.items();
  }
}