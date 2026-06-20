package com.sisibibi.api.domain.topic.service;

import com.sisibibi.api.domain.topic.dto.response.issueRes.*;
import com.sisibibi.api.domain.topic.dto.response.keywordres.ClassifiedIssueCandidateRes;
import com.sisibibi.api.domain.topic.dto.response.keywordres.ClassifiedIssueNewsRes;
import com.sisibibi.api.domain.topic.dto.response.keywordres.ClassifiedNewsKeywordRes;
import com.sisibibi.api.domain.topic.dto.response.keywordres.NewsKeywordClassificationResult;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
public class TopicKeywordService {

  private final ChatClient chatClient;

  public TopicKeywordService(ChatClient.Builder chatClientBuilder) {
    this.chatClient = chatClientBuilder.build();
  }

  public List<ClassifiedIssueCandidateRes> classify(List<IssueCandidateRes> candidates) {
    return candidates.stream()
        .map(this::classify)
        .toList();
  }

  private ClassifiedIssueCandidateRes classify(IssueCandidateRes candidate) {
    NewsKeywordClassificationResult result = chatClient.prompt()
        .system("""
            너는 한국어 뉴스 분류 도우미다.
            입력된 토픽 키워드와 뉴스 목록을 보고 뉴스별 핵심 키워드를 분류한다.
            반드시 입력 뉴스 index를 유지한다.
            keywords는 1개 이상 5개 이하로 작성한다.
            category는 정치, 경제, 사회, 국제, IT, 과학, 문화, 스포츠, 연예, 기타 중 하나로 작성한다.
            """)
        .user(user -> user.text("""
            토픽 키워드: {topicKeyword}

            뉴스 목록:
            {news}
            """)
            .param("topicKeyword", candidate.keyword())
            .param("news", toPromptNews(candidate.news())))
        .call()
        .entity(NewsKeywordClassificationResult.class);

    Map<Integer, ClassifiedNewsKeywordRes> classifiedByIndex = result.news().stream()
        .collect(Collectors.toMap(
            ClassifiedNewsKeywordRes::index,
            Function.identity(),
            (left, right) -> left
        ));

    List<ClassifiedIssueNewsRes> classifiedNews = IntStream.range(0, candidate.news().size())
        .mapToObj(index -> {
          IssueNewsRes news = candidate.news().get(index);
          ClassifiedNewsKeywordRes classified = classifiedByIndex.get(index);

          return new ClassifiedIssueNewsRes(
              news,
              classified == null ? "기타" : classified.category(),
              classified == null ? List.of(candidate.keyword()) : classified.keywords()
          );
        })
        .toList();

    return new ClassifiedIssueCandidateRes(
        candidate.keyword(),
        candidate.searchVolume(),
        candidate.increasePercentage(),
        classifiedNews
    );
  }

  private String toPromptNews(List<IssueNewsRes> news) {
    return IntStream.range(0, news.size())
        .mapToObj(index -> """
            index: %d
            title: %s
            description: %s
            pubDate: %s
            """.formatted(
            index,
            news.get(index).title(),
            news.get(index).description(),
            news.get(index).pubDate()
        ))
        .collect(Collectors.joining("\n"));
  }
}