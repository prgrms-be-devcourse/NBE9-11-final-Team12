package com.sisibibi.api.domain.topic.controller;

import com.sisibibi.api.domain.topic.dto.response.issueRes.IssueCandidateRes;
import com.sisibibi.api.domain.topic.dto.response.topicRes.TopicDetailRes;
import com.sisibibi.api.domain.topic.service.TopicIssueService;
import com.sisibibi.api.domain.topic.service.TopicService;
import com.sisibibi.api.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
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
  private final TopicService topicService;

  @GetMapping("/{topicId}")
  public ResponseEntity<ApiResponse<TopicDetailRes>> getTopicDetail(
      @PathVariable Long topicId
  ) {
    TopicDetailRes result = topicService.getTopicDetail(topicId);

    return ResponseEntity.ok(ApiResponse.ok("토픽 상세 조회가 완료되었습니다.", result));
  }

  @GetMapping("/candidates")
  public ResponseEntity<ApiResponse<List<IssueCandidateRes>>> createIssue() {
    List<IssueCandidateRes> result = topicIssueService.createIssue();

    return ResponseEntity.ok(ApiResponse.ok("실시간 이슈 후보 생성이 완료되었습니다.", result));
  }

}