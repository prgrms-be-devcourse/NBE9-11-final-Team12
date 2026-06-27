package com.sisibibi.api.domain.topic.controller;

import com.sisibibi.api.domain.topic.dto.response.issueRes.IssueCandidateRes;
import com.sisibibi.api.domain.topic.dto.response.keywordres.ClassifiedIssueCandidateRes;
import com.sisibibi.api.domain.topic.dto.response.topicRes.TopicDetailRes;
import com.sisibibi.api.domain.topic.dto.response.topicRes.TopicSummaryRes;
import com.sisibibi.api.domain.topic.service.TopicIssueService;
import com.sisibibi.api.domain.topic.service.TopicKeywordService;
import com.sisibibi.api.domain.topic.service.TopicService;
import com.sisibibi.api.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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

@Tag(name = "토픽", description = "토픽 조회 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/topics/issues")
public class TopicController {

  private final TopicIssueService topicIssueService;
  private final TopicKeywordService topicKeywordService;
  private final TopicService topicService;

  @Operation(
      summary = "토픽 상세 조회",
      description = "승인된 토픽의 상세 정보를 조회합니다."
  )
  @GetMapping("/{topicId}")
  public ResponseEntity<ApiResponse<TopicDetailRes>> getTopicDetail(
      @PathVariable Long topicId
  ) {
    TopicDetailRes result = topicService.getTopicDetail(topicId);

    return ResponseEntity.ok(ApiResponse.ok("토픽 상세 조회가 완료되었습니다.", result));
  }

  @Operation(
      summary = "승인된 토픽 목록 조회",
      description = "승인된 토픽 목록을 페이지 단위로 조회합니다."
  )
  @GetMapping
  public ResponseEntity<ApiResponse<Page<TopicSummaryRes>>> getApprovedTopics(
      @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
      Pageable pageable
  ) {
    Page<TopicSummaryRes> result = topicService.getApprovedTopics(pageable);

    return ResponseEntity.ok(ApiResponse.ok("승인된 토픽 목록 조회가 완료되었습니다.", result));
  }

}