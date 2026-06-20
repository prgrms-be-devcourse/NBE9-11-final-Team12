package com.sisibibi.api.domain.topic.controller;

import com.sisibibi.api.domain.topic.dto.request.CreateTopicReq;
import com.sisibibi.api.domain.topic.dto.request.UpdateTopicReq;
import com.sisibibi.api.domain.topic.dto.response.issueRes.IssueCandidateRes;
import com.sisibibi.api.domain.topic.dto.response.keywordres.ClassifiedIssueCandidateRes;
import com.sisibibi.api.domain.topic.dto.response.topicRes.TopicCreateRes;
import com.sisibibi.api.domain.topic.dto.response.topicRes.TopicDetailRes;
import com.sisibibi.api.domain.topic.service.TopicIssueService;
import com.sisibibi.api.domain.topic.service.TopicKeywordService;
import com.sisibibi.api.domain.topic.service.TopicService;
import com.sisibibi.api.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/topics")
public class AdminTopicController {

  private final TopicService topicService;
  private final TopicKeywordService topicKeywordService;
  private final TopicIssueService topicIssueService;

  @PostMapping
  public ResponseEntity<ApiResponse<TopicCreateRes>> createApprovedTopic(
      @Valid @RequestBody CreateTopicReq request
  ) {
    TopicCreateRes result = topicService.createApprovedTopic(request);

    return ResponseEntity
        .status(HttpStatus.CREATED)
        .body(ApiResponse.created("선택한 후보 토픽이 승인되어 저장되었습니다.", result));
  }

  @PatchMapping("/{topicId}")
  public ResponseEntity<ApiResponse<TopicDetailRes>> updateTopic(
      @PathVariable Long topicId,
      @Valid @RequestBody UpdateTopicReq request
  ) {
    TopicDetailRes result = topicService.updateTopic(topicId, request);

    return ResponseEntity.ok(ApiResponse.ok("토픽 수정이 완료되었습니다.", result));
  }

  @DeleteMapping("/{topicId}")
  public ResponseEntity<ApiResponse<Void>> deleteTopic(
      @PathVariable Long topicId
  ) {
    topicService.deleteTopic(topicId);

    return ResponseEntity.ok(ApiResponse.okMessage("토픽 삭제가 완료되었습니다."));
  }

  // 관리자용
  @GetMapping("/candidates/classified")
  public ResponseEntity<ApiResponse<List<ClassifiedIssueCandidateRes>>> createClassifiedIssue() {
    List<IssueCandidateRes> candidates = topicIssueService.createIssue();
    List<ClassifiedIssueCandidateRes> result = topicKeywordService.classify(candidates);

    return ResponseEntity.ok(ApiResponse.ok("뉴스별 키워드 분류가 완료되었습니다.", result));
  }
}