package com.sisibibi.api.domain.speech.controller;

import com.sisibibi.api.domain.speech.dto.request.SpeechCreateReq;
import com.sisibibi.api.domain.speech.dto.response.SpeechCreateRes;
import com.sisibibi.api.domain.speech.service.SpeechService;
import com.sisibibi.api.global.response.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/rooms/{roomId}/speeches")
public class SpeechController {

    private final SpeechService speechService;

    @PostMapping
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
}
