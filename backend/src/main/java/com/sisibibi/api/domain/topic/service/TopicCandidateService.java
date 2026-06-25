package com.sisibibi.api.domain.topic.service;

import com.sisibibi.api.domain.topic.dto.response.issueRes.IssueCandidateRes;
import com.sisibibi.api.domain.topic.dto.response.keywordres.ClassifiedIssueCandidateRes;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TopicCandidateService {

  private static final String LATEST_TOPIC_CANDIDATES_KEY = "'latest'";

  private final TopicIssueService topicIssueService;
  private final TopicKeywordService topicKeywordService;

  @Cacheable(value = "topicCandidates", key = LATEST_TOPIC_CANDIDATES_KEY)
  public List<ClassifiedIssueCandidateRes> getLatestClassifiedCandidates() {
    return createClassifiedCandidates();
  }

  @CachePut(value = "topicCandidates", key = LATEST_TOPIC_CANDIDATES_KEY)
  public List<ClassifiedIssueCandidateRes> refreshLatestClassifiedCandidates() {
    return createClassifiedCandidates();
  }

  private List<ClassifiedIssueCandidateRes> createClassifiedCandidates() {
    List<IssueCandidateRes> candidates = topicIssueService.createIssue();
    return topicKeywordService.classify(candidates);
  }
}