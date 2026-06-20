package com.sisibibi.api.domain.topic.controller;

import com.sisibibi.api.domain.topic.dto.response.issueRes.IssueCandidateRes;
import com.sisibibi.api.domain.topic.dto.response.keywordres.ClassifiedIssueCandidateRes;
import com.sisibibi.api.domain.topic.dto.response.topicRes.TopicDetailRes;
import com.sisibibi.api.domain.topic.dto.response.topicRes.TopicSummaryRes;
import com.sisibibi.api.domain.topic.service.TopicIssueService;
import com.sisibibi.api.domain.topic.service.TopicKeywordService;
import com.sisibibi.api.domain.topic.service.TopicService;
import com.sisibibi.api.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/topics/issues")
public class TopicController {

  private final TopicIssueService topicIssueService;
  private final TopicKeywordService topicKeywordService;
  private final TopicService topicService;

  @GetMapping("/{topicId}")
  public ResponseEntity<ApiResponse<TopicDetailRes>> getTopicDetail(
      @PathVariable Long topicId
  ) {
    TopicDetailRes result = topicService.getTopicDetail(topicId);

    return ResponseEntity.ok(ApiResponse.ok("토픽 상세 조회가 완료되었습니다.", result));
  }

  @GetMapping
  public ResponseEntity<ApiResponse<Page<TopicSummaryRes>>> getApprovedTopics(
      @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
      Pageable pageable
  ) {
    Page<TopicSummaryRes> result = topicService.getApprovedTopics(pageable);

    return ResponseEntity.ok(ApiResponse.ok("승인된 토픽 목록 조회가 완료되었습니다.", result));
  }

  // 테스트용
  @GetMapping("/candidates/classified")
  public ResponseEntity<ApiResponse<List<ClassifiedIssueCandidateRes>>> createClassifiedIssue() {
    List<IssueCandidateRes> candidates = topicIssueService.createIssue();
    List<ClassifiedIssueCandidateRes> result = topicKeywordService.classify(candidates);

    return ResponseEntity.ok(ApiResponse.ok("뉴스별 키워드 분류가 완료되었습니다.", result));
  }

  // ai 분류 없는 토픽후보 모음
  @GetMapping("/candidates")
  public ResponseEntity<ApiResponse<List<IssueCandidateRes>>> createIssue() {
    List<IssueCandidateRes> result = topicIssueService.createIssue();

    return ResponseEntity.ok(ApiResponse.ok("실시간 이슈 후보 생성이 완료되었습니다.", result));
  }


}