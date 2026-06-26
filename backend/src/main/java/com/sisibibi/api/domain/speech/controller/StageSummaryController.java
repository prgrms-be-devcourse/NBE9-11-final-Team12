package com.sisibibi.api.domain.speech.controller;

import com.sisibibi.api.domain.speech.dto.response.StageSummaryRes;
import com.sisibibi.api.domain.speech.service.StageSummaryPersistenceService;
import com.sisibibi.api.global.response.ApiResponse;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/rooms/{roomId}/stage-summary")
public class StageSummaryController {

    private final StageSummaryPersistenceService stageSummaryPersistenceService;

    @GetMapping
    public ResponseEntity<ApiResponse<StageSummaryRes>> getStageSummary(
            @PathVariable @Positive Long roomId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                StageSummaryRes.from(stageSummaryPersistenceService.getSummary(roomId))
        ));
    }
}
