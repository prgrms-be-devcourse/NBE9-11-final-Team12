package com.sisibibi.api.domain.speech.controller;

import com.sisibibi.api.domain.speech.dto.request.SpeechCreateReq;
import com.sisibibi.api.domain.speech.dto.response.SpeechCreateRes;
import com.sisibibi.api.domain.speech.dto.response.SpeechCursorPageRes;
import com.sisibibi.api.domain.speech.dto.response.SpeechDetailRes;
import com.sisibibi.api.domain.speech.service.SpeechService;
import com.sisibibi.api.global.response.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class SpeechController {

    private final SpeechService speechService;

    @PostMapping("/rooms/{roomId}/speeches")
    public ResponseEntity<ApiResponse<SpeechCreateRes>> createMainOpinion(
            @PathVariable @Positive Long roomId,
            @RequestHeader("X-User-Id") @Positive Long userId,
            @Valid @RequestBody SpeechCreateReq request
    ) {
        SpeechCreateRes response = speechService.createMainOpinion(
                roomId,
                userId,
                request.toCommand()
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.created("메인 의견 작성이 완료되었습니다.", response));
    }

    @GetMapping("/rooms/{roomId}/speeches")
    public ResponseEntity<ApiResponse<SpeechCursorPageRes>> getSpeeches(
            @PathVariable @Positive Long roomId,
            @RequestParam(required = false) @Positive Long cursor,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                "의견 목록 조회가 완료되었습니다.",
                speechService.getSpeeches(roomId, cursor, size)
        ));
    }

    @GetMapping("/speeches/{speechId}")
    public ResponseEntity<ApiResponse<SpeechDetailRes>> getSpeech(
            @PathVariable @Positive Long speechId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                "의견 상세 조회가 완료되었습니다.",
                speechService.getSpeech(speechId)
        ));
    }
}
