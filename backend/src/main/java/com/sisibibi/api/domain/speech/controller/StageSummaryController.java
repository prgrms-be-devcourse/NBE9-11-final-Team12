package com.sisibibi.api.domain.speech.controller;

import com.sisibibi.api.domain.speech.dto.response.StageSummaryRes;
import com.sisibibi.api.domain.speech.service.StageSummaryPersistenceService;
import com.sisibibi.api.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "발언 요약", description = "토론방 발언 요약 조회 API")
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/rooms/{roomId}/stage-summary")
public class StageSummaryController {

    private final StageSummaryPersistenceService stageSummaryPersistenceService;

    @Operation(
        summary = "발언 요약 조회",
        description = "지정한 토론방의 발언 요약 정보를 조회합니다."
    )
    @GetMapping
    public ResponseEntity<ApiResponse<StageSummaryRes>> getStageSummary(
            @PathVariable @Positive Long roomId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                StageSummaryRes.from(stageSummaryPersistenceService.getSummary(roomId))
        ));
    }
}
