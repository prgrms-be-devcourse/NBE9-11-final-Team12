package com.sisibibi.api.domain.topic.service;

import com.sisibibi.api.domain.topic.dto.request.NewsSearchCommand;
import com.sisibibi.api.domain.topic.dto.response.issueRes.GoogleTrendsRes;
import com.sisibibi.api.domain.topic.dto.response.issueRes.IssueCandidateRes;
import com.sisibibi.api.domain.topic.dto.response.issueRes.IssueNewsRes;
import com.sisibibi.api.domain.topic.dto.response.issueRes.NewsSearchRes;
import com.sisibibi.api.domain.topic.dto.response.issueRes.TrendingSearchRes;
import com.sisibibi.api.global.client.naverApi.NewsClient;
import com.sisibibi.api.global.client.serpApi.GoogleTrendsClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TopicIssueServiceTest {

  @Mock
  private GoogleTrendsClient googleTrendsClient;

  @Mock
  private NewsClient naverNewsClient;

  @InjectMocks
  private TopicIssueService topicIssueService;

  @Test
  void createIssue_returnsEmptyList_whenTrendingSearchesIsNull() {
    given(googleTrendsClient.getTrendingNow())
        .willReturn(new GoogleTrendsRes(null));

    List<IssueCandidateRes> result = topicIssueService.createIssue();

    assertThat(result).isEmpty();
    verify(naverNewsClient, never()).search(org.mockito.ArgumentMatchers.any());
  }

  @Test
  void createIssue_skipsBlankKeyword() {
    given(googleTrendsClient.getTrendingNow())
        .willReturn(new GoogleTrendsRes(List.of(
            new TrendingSearchRes("   ", 1000L, 20)
        )));

    List<IssueCandidateRes> result = topicIssueService.createIssue();

    assertThat(result).isEmpty();
    verify(naverNewsClient, never()).search(org.mockito.ArgumentMatchers.any());
  }

  @Test
  void createIssue_skipsKeyword_whenNewsCountIsLessThanThree() {
    given(googleTrendsClient.getTrendingNow())
        .willReturn(new GoogleTrendsRes(List.of(
            new TrendingSearchRes("AI", 1000L, 20)
        )));
    given(naverNewsClient.search(org.mockito.ArgumentMatchers.any()))
        .willReturn(new NewsSearchRes(
            "now",
            2,
            1,
            2,
            List.of(
                news("AI news 1"),
                news("AI news 2")
            )
        ));

    List<IssueCandidateRes> result = topicIssueService.createIssue();

    assertThat(result).isEmpty();
  }

  @Test
  void createIssue_returnsIssueCandidate_whenKeywordHasThreeNews() {
    given(googleTrendsClient.getTrendingNow())
        .willReturn(new GoogleTrendsRes(List.of(
            new TrendingSearchRes("AI", 1000L, 20)
        )));
    given(naverNewsClient.search(org.mockito.ArgumentMatchers.any()))
        .willReturn(new NewsSearchRes(
            "now",
            3,
            1,
            3,
            List.of(
                news("AI news 1"),
                news("AI news 2"),
                news("AI news 3")
            )
        ));

    List<IssueCandidateRes> result = topicIssueService.createIssue();

    assertThat(result).hasSize(1);
    assertThat(result.get(0).keyword()).isEqualTo("AI");
    assertThat(result.get(0).searchVolume()).isEqualTo(1000L);
    assertThat(result.get(0).increasePercentage()).isEqualTo(20);
    assertThat(result.get(0).news()).hasSize(3);

    ArgumentCaptor<NewsSearchCommand> captor = ArgumentCaptor.forClass(NewsSearchCommand.class);
    verify(naverNewsClient).search(captor.capture());

    NewsSearchCommand command = captor.getValue();
    assertThat(command.query()).isEqualTo("AI");
    assertThat(command.display()).isEqualTo(3);
    assertThat(command.start()).isEqualTo(1);
    assertThat(command.sort()).isEqualTo("date");
  }

  @Test
  void createIssue_returnsOnlyTenCandidates() {
    List<TrendingSearchRes> trends = new ArrayList<>();
    for (int index = 1; index <= 12; index++) {
      trends.add(new TrendingSearchRes("keyword" + index, 1000L, 20));
    }

    given(googleTrendsClient.getTrendingNow())
        .willReturn(new GoogleTrendsRes(trends));
    given(naverNewsClient.search(org.mockito.ArgumentMatchers.any()))
        .willReturn(new NewsSearchRes(
            "now",
            3,
            1,
            3,
            List.of(
                news("news 1"),
                news("news 2"),
                news("news 3")
            )
        ));

    List<IssueCandidateRes> result = topicIssueService.createIssue();

    assertThat(result).hasSize(10);
  }

  @Test
  void createIssue_returnsEmptyNews_whenNaverItemsIsNull() {
    given(googleTrendsClient.getTrendingNow())
        .willReturn(new GoogleTrendsRes(List.of(
            new TrendingSearchRes("AI", 1000L, 20)
        )));
    given(naverNewsClient.search(org.mockito.ArgumentMatchers.any()))
        .willReturn(new NewsSearchRes("now", 0, 1, 0, null));

    List<IssueCandidateRes> result = topicIssueService.createIssue();

    assertThat(result).isEmpty();
  }

  private IssueNewsRes news(String title) {
    return new IssueNewsRes(
        title,
        "https://original.example.com/" + title,
        "https://news.example.com/" + title,
        "description",
        "Tue, 16 Jun 2026 10:00:00 +0900"
    );
  }
}