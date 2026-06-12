package com.sisibibi.api.domain.topic.controller;

import com.sisibibi.api.domain.topic.dto.request.CreateTopicReq;
import com.sisibibi.api.domain.topic.dto.response.topicRes.TopicCreateRes;
import com.sisibibi.api.domain.topic.service.TopicService;
import com.sisibibi.api.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/topics")
public class AdminTopicController {

  private final TopicService topicService;

  @PostMapping("/{topicId}/approve")
  public ResponseEntity<ApiResponse<TopicCreateRes>> createPendingTopic(
      @Valid @RequestBody CreateTopicReq request
  ) {
    TopicCreateRes result = topicService.createApprovedTopic(request);

    return ResponseEntity
        .status(HttpStatus.CREATED)
        .body(ApiResponse.created("토픽 후보가 저장되었습니다.", result));
  }
}