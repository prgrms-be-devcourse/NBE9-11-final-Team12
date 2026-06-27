package com.sisibibi.api.domain.speech.controller;

import com.sisibibi.api.domain.speech.dto.response.AiCounterIssueRes;
import com.sisibibi.api.domain.speech.service.AiCounterIssuePersistenceService;
import com.sisibibi.api.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "AI 반론 이슈", description = "토론방 AI 반론 이슈 조회 API")
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/rooms/{roomId}/ai-counter-issues")
public class AiCounterIssueController {

    private final AiCounterIssuePersistenceService aiCounterIssuePersistenceService;

    @Operation(
        summary = "최근 AI 반론 이슈 조회",
        description = "지정한 토론방에서 생성 완료된 최근 AI 반론 이슈 목록을 조회합니다."
    )
    @GetMapping("/recent")
    public ResponseEntity<ApiResponse<List<AiCounterIssueRes>>> getRecentCounterIssues(
            @PathVariable @Positive Long roomId
    ) {
        List<AiCounterIssueRes> response = aiCounterIssuePersistenceService
                .findRecentCompleted(roomId)
                .stream()
                .map(AiCounterIssueRes::from)
                .toList();

        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
