package com.sisibibi.api.domain.speech.controller;

import com.sisibibi.api.domain.speech.dto.response.StageRequestRes;
import com.sisibibi.api.domain.speech.service.SpeakingQueueService;
import com.sisibibi.api.global.response.ApiResponse;
import com.sisibibi.api.global.security.AuthPrincipal;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/rooms/{roomId}/stage")
public class StageController {

    private final SpeakingQueueService speakingQueueService;

    @PostMapping("/requests")
    public ResponseEntity<ApiResponse<StageRequestRes>> requestSpeakingTurn(
            @PathVariable @Positive Long roomId,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        StageRequestRes response = speakingQueueService.requestSpeakingTurn(roomId, principal.userId());

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.created("발언권 신청이 완료되었습니다.", response));
    }

    @DeleteMapping("/requests/me")
    public ResponseEntity<ApiResponse<Void>> cancelSpeakingRequest(
            @PathVariable @Positive Long roomId,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        speakingQueueService.cancelSpeakingRequest(roomId, principal.userId());

        return ResponseEntity.ok(
                ApiResponse.okMessage("발언권 신청이 취소되었습니다.")
        );
    }
}
