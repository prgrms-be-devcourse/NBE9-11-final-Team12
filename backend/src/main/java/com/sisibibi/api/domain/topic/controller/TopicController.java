package com.sisibibi.api.domain.topic.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.sisibibi.api.domain.topic.dto.request.NewsSearchCommand;
import com.sisibibi.api.domain.topic.dto.response.IssueCandidateRes;
import com.sisibibi.api.domain.topic.service.TopicIssueService;
import com.sisibibi.api.global.response.ApiResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/topics/issues")
public class TopicController {

  private final TopicIssueService topicIssueService;

  @GetMapping("/candidates")
  public ResponseEntity<ApiResponse<List<IssueCandidateRes>>> createIssue() {
    List<IssueCandidateRes> result = topicIssueService.createIssue();

    return ResponseEntity.ok(ApiResponse.ok("실시간 이슈 후보 생성이 완료되었습니다.", result));
  }
}