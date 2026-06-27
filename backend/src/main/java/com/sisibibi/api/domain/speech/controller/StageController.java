package com.sisibibi.api.domain.speech.controller;

import com.sisibibi.api.domain.speech.dto.request.StageRequestReq;
import com.sisibibi.api.domain.speech.dto.response.StageCurrentSpeakerRes;
import com.sisibibi.api.domain.speech.dto.response.StageQueueRes;
import com.sisibibi.api.domain.speech.dto.response.StageRequestRes;
import com.sisibibi.api.domain.speech.dto.response.StageRequestStatusRes;
import com.sisibibi.api.domain.speech.service.SpeakingQueueService;
import com.sisibibi.api.global.response.ApiResponse;
import com.sisibibi.api.global.security.AuthPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "발언 스테이지", description = "토론방 발언자, 발언권 요청, 발언 대기열 API")
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/rooms/{roomId}/stage")
public class StageController {

    private final SpeakingQueueService speakingQueueService;

    @Operation(
        summary = "현재 발언자 조회",
        description = "지정한 토론방의 현재 발언자 정보를 조회합니다."
    )
    @GetMapping
    public ResponseEntity<ApiResponse<StageCurrentSpeakerRes>> getCurrentSpeaker(
            @PathVariable @Positive Long roomId
    ) {
        StageCurrentSpeakerRes response =
                speakingQueueService.getCurrentSpeaker(roomId);

        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @Operation(
        summary = "발언 대기열 요약 조회",
        description = "지정한 토론방의 발언 대기열 요약 정보를 조회합니다."
    )
    @GetMapping("/queue/summary")
    public ResponseEntity<ApiResponse<StageQueueRes>> getQueueSummary(
            @PathVariable @Positive Long roomId
    ) {
        StageQueueRes response = speakingQueueService.getQueueSummary(roomId);

        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @Operation(
        summary = "발언 대기열 조회",
        description = "지정한 토론방의 발언 대기열을 offset과 size 기준으로 조회합니다."
    )
    @GetMapping("/queue")
    public ResponseEntity<ApiResponse<StageQueueRes>> getWaitingQueue(
            @PathVariable @Positive Long roomId,
            @RequestParam(required = false) @PositiveOrZero Integer offset,
            @RequestParam(required = false) @Positive Integer size
    ) {
        StageQueueRes response =
                speakingQueueService.getWaitingQueue(roomId, offset, size);

        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @Operation(
        summary = "발언권 신청",
        description = "현재 로그인 사용자가 지정한 토론방에서 발언권을 신청합니다."
    )
    @PostMapping("/requests")
    public ResponseEntity<ApiResponse<StageRequestRes>> requestSpeakingTurn(
            @PathVariable @Positive Long roomId,
            @Valid @RequestBody StageRequestReq request,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {


        StageRequestRes response = speakingQueueService.requestSpeakingTurn(
                roomId,
                principal.userId(),
                request.stance()
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.created("발언권 신청이 완료되었습니다.", response));
    }

    @Operation(
        summary = "내 발언권 신청 취소",
        description = "현재 로그인 사용자의 발언권 신청을 취소합니다."
    )
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

    @Operation(
        summary = "내 발언권 신청 상태 조회",
        description = "현재 로그인 사용자의 발언권 신청 상태를 조회합니다."
    )
    @GetMapping("/requests/me")
    public ResponseEntity<ApiResponse<StageRequestStatusRes>> getMySpeakingRequestStatus(
            @PathVariable @Positive Long roomId,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        StageRequestStatusRes response =
                speakingQueueService.getMySpeakingRequestStatus(
                        roomId,
                        principal.userId()
                );

        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @Operation(
        summary = "발언 종료",
        description = "현재 로그인 사용자의 진행 중인 발언을 종료합니다."
    )
    @PostMapping("/complete")
    public ResponseEntity<ApiResponse<Void>> completeSpeakingTurn(
            @PathVariable @Positive Long roomId,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {


        speakingQueueService.completeSpeakingTurn(roomId, principal.userId());

        return ResponseEntity.ok(
                ApiResponse.okMessage("발언이 종료되었습니다.")
        );
    }
}
