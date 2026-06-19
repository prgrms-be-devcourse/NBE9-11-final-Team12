package com.sisibibi.api.domain.speechreaction.controller;

import com.sisibibi.api.domain.speechreaction.dto.response.SpeechReactionCreateRes;
import com.sisibibi.api.domain.speechreaction.service.SpeechReactionService;
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
@RequestMapping("/api/v1/speeches")
public class SpeechReactionController {

    private final SpeechReactionService speechReactionService;

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

    @DeleteMapping("/{speechId}/reactions")
    public ResponseEntity<ApiResponse<Void>> deleteReaction(
            @PathVariable @Positive Long speechId,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        speechReactionService.deleteReaction(speechId, principal.userId());
        return ResponseEntity.ok(ApiResponse.okMessage("의견 공감이 취소되었습니다."));
    }
}
