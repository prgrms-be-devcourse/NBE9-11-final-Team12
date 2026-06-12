package com.sisibibi.api.domain.speech.controller;

import com.sisibibi.api.domain.speech.dto.response.CurrentSpeakerRes;
import com.sisibibi.api.domain.speech.dto.response.StageCompleteRes;
import com.sisibibi.api.domain.speech.dto.response.StageQueueRes;
import com.sisibibi.api.domain.speech.dto.response.StageRequestRes;
import com.sisibibi.api.domain.speech.service.SpeakingQueueService;
import com.sisibibi.api.global.response.ApiResponse;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
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

    @GetMapping("/requests/me")
    public ResponseEntity<ApiResponse<StageRequestRes>> getMyRequest(
            @Positive @PathVariable Long roomId,
            @Positive @RequestParam Long userId
    ) {
        StageRequestRes response = speakingQueueService.getMyRequest(roomId, userId);

        return ResponseEntity.ok(ApiResponse.ok("발언권 신청 상태 조회가 완료되었습니다.", response));
    }

    @DeleteMapping("/requests/me")
    public ResponseEntity<ApiResponse<Void>> cancelMyRequest(
            @Positive @PathVariable Long roomId,
            @Positive @RequestParam Long userId
    ) {
        speakingQueueService.cancelMyRequest(roomId, userId);

        return ResponseEntity.ok(ApiResponse.okMessage("발언권 신청 취소가 완료되었습니다."));
    }

    @GetMapping("/queue")
    public ResponseEntity<ApiResponse<StageQueueRes>> getWaitingQueue(
            @Positive @PathVariable Long roomId
    ) {
        StageQueueRes response = speakingQueueService.getWaitingQueue(roomId);

        return ResponseEntity.ok(ApiResponse.ok("발언 대기열 조회가 완료되었습니다.", response));
    }

    @PostMapping("/complete")
    public ResponseEntity<ApiResponse<StageCompleteRes>> completeCurrentSpeaker(
            @Positive @PathVariable Long roomId,
            @Positive @RequestParam Long userId
    ) {
        StageCompleteRes response = speakingQueueService.completeCurrentSpeaker(roomId, userId);

        return ResponseEntity.ok(ApiResponse.ok("발언 종료가 완료되었습니다.", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<CurrentSpeakerRes>> getCurrentSpeaker(
            @Positive @PathVariable Long roomId
    ) {
        CurrentSpeakerRes response = speakingQueueService.getCurrentSpeaker(roomId);

        return ResponseEntity.ok(ApiResponse.ok("현재 발언자 조회가 완료되었습니다.", response));
    }
}
