package com.sisibibi.api.domain.speech.controller;

import com.sisibibi.api.domain.speech.dto.response.StageRequestRes;
import com.sisibibi.api.domain.speech.service.SpeakingQueueService;
import com.sisibibi.api.global.response.ApiResponse;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/rooms/{roomId}/stage")
public class StageController {

    private final SpeakingQueueService speakingQueueService;

    @PostMapping("/requests")
    public ResponseEntity<ApiResponse<StageRequestRes>> requestSpeakingTurn(
            @Positive @PathVariable Long roomId,
            @Positive @RequestParam Long userId
    ) {
        StageRequestRes response = speakingQueueService.requestSpeakingTurn(roomId, userId);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.created("발언권 신청이 완료되었습니다.", response));
    }
}
