package com.sisibibi.api.domain.topic.controller;

import com.sisibibi.api.domain.topic.dto.request.CreateTopicReq;
import com.sisibibi.api.domain.topic.dto.request.UpdateTopicReq;
import com.sisibibi.api.domain.topic.dto.response.issueRes.IssueCandidateRes;
import com.sisibibi.api.domain.topic.dto.response.keywordres.ClassifiedIssueCandidateRes;
import com.sisibibi.api.domain.topic.dto.response.topicRes.TopicCreateRes;
import com.sisibibi.api.domain.topic.dto.response.topicRes.TopicDetailRes;
import com.sisibibi.api.domain.topic.service.TopicCandidateService;
import com.sisibibi.api.domain.topic.service.TopicIssueService;
import com.sisibibi.api.domain.topic.service.TopicKeywordService;
import com.sisibibi.api.domain.topic.service.TopicService;
import com.sisibibi.api.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "관리자 토픽", description = "관리자 토픽 관리 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/topics")
public class AdminTopicController {

  private final TopicService topicService;
  private final TopicKeywordService topicKeywordService;
  private final TopicIssueService topicIssueService;
  private final TopicCandidateService topicCandidateService;

  @Operation(
      summary = "승인 토픽 생성",
      description = "관리자가 토픽 후보를 승인된 토픽으로 저장합니다."
  )
  @PostMapping
  public ResponseEntity<ApiResponse<TopicCreateRes>> createApprovedTopic(
      @Valid @RequestBody CreateTopicReq request
  ) {
    TopicCreateRes result = topicService.createApprovedTopic(request);

    return ResponseEntity
        .status(HttpStatus.CREATED)
        .body(ApiResponse.created("선택한 후보 토픽이 승인되어 저장되었습니다.", result));
  }

  @Operation(
      summary = "토픽 수정",
      description = "관리자가 기존 토픽 정보를 수정합니다."
  )
  @PatchMapping("/{topicId}")
  public ResponseEntity<ApiResponse<TopicDetailRes>> updateTopic(
      @PathVariable Long topicId,
      @Valid @RequestBody UpdateTopicReq request
  ) {
    TopicDetailRes result = topicService.updateTopic(topicId, request);

    return ResponseEntity.ok(ApiResponse.ok("토픽 수정이 완료되었습니다.", result));
  }

  @Operation(
      summary = "토픽 삭제",
      description = "관리자가 토픽을 삭제합니다."
  )
  @DeleteMapping("/{topicId}")
  public ResponseEntity<ApiResponse<Void>> deleteTopic(
      @PathVariable Long topicId
  ) {
    topicService.deleteTopic(topicId);

    return ResponseEntity.ok(ApiResponse.okMessage("토픽 삭제가 완료되었습니다."));
  }

  @Operation(
      summary = "분류된 토픽 후보 조회",
      description = "최근 분류된 토픽 후보 목록을 조회합니다."
  )
  @GetMapping("/candidates/classified")
  public ResponseEntity<ApiResponse<List<ClassifiedIssueCandidateRes>>> getClassifiedCandidates() {
    List<ClassifiedIssueCandidateRes> result =
        topicCandidateService.getLatestClassifiedCandidates();

    return ResponseEntity.ok(ApiResponse.ok("캐시된 토픽 후보 조회가 완료되었습니다.", result));
  }

  @Operation(
      summary = "분류된 토픽 후보 새로고침",
      description = "최신 이슈를 기반으로 토픽 후보를 다시 분류하고 조회합니다."
  )
  @PostMapping("/candidates/classified/refresh")
  public ResponseEntity<ApiResponse<List<ClassifiedIssueCandidateRes>>> refreshClassifiedCandidates() {
    List<ClassifiedIssueCandidateRes> result =
        topicCandidateService.refreshLatestClassifiedCandidates();

    return ResponseEntity.ok(ApiResponse.ok("토픽 후보 새로고침이 완료되었습니다.", result));
  }
}