package com.sisibibi.api.domain.speechreaction.controller;

import com.sisibibi.api.domain.speechreaction.dto.response.SpeechReactionCreateRes;
import com.sisibibi.api.domain.speechreaction.service.SpeechReactionService;
import com.sisibibi.api.global.response.ApiResponse;
import com.sisibibi.api.global.security.AuthPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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

@Tag(name = "의견 공감", description = "의견 공감 등록 및 취소 API")
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/speeches")
public class SpeechReactionController {

    private final SpeechReactionService speechReactionService;

    @Operation(
        summary = "의견 공감 등록",
        description = "현재 로그인 사용자가 지정한 의견에 공감을 등록합니다."
    )
    @PostMapping("/{speechId}/reactions")
    public ResponseEntity<ApiResponse<SpeechReactionCreateRes>> createReaction(
            @PathVariable @Positive Long speechId,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        SpeechReactionCreateRes response = speechReactionService.createReaction(
                speechId,
                principal.userId()
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.created("의견 공감이 등록되었습니다.", response));
    }

    @Operation(
        summary = "의견 공감 취소",
        description = "현재 로그인 사용자가 지정한 의견에 등록한 공감을 취소합니다."
    )
    @DeleteMapping("/{speechId}/reactions")
    public ResponseEntity<ApiResponse<Void>> deleteReaction(
            @PathVariable @Positive Long speechId,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        speechReactionService.deleteReaction(speechId, principal.userId());
        return ResponseEntity.ok(ApiResponse.okMessage("의견 공감이 취소되었습니다."));
    }
}
