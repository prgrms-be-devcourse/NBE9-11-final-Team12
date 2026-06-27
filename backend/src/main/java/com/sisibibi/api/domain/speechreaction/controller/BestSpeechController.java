package com.sisibibi.api.domain.speechreaction.controller;

import com.sisibibi.api.domain.speechreaction.dto.response.BestSpeechRes;
import com.sisibibi.api.domain.speechreaction.service.SpeechReactionService;
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

@Tag(name = "베스트 의견", description = "토론방 베스트 의견 조회 API")
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/rooms")
public class BestSpeechController {

    private final SpeechReactionService speechReactionService;

    @Operation(
        summary = "베스트 의견 조회",
        description = "지정한 토론방에서 가장 많은 공감을 받은 베스트 의견을 조회합니다."
    )
    @GetMapping("/{roomId}/best-speech")
    public ResponseEntity<ApiResponse<BestSpeechRes>> getBestSpeech(
            @PathVariable @Positive Long roomId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                "베스트 의견 조회가 완료되었습니다.",
                speechReactionService.getBestSpeech(roomId)
        ));
    }
}
