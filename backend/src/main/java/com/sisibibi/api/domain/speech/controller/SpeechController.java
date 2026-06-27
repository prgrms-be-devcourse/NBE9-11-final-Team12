package com.sisibibi.api.domain.speech.controller;

import com.sisibibi.api.domain.speech.dto.request.*;
import com.sisibibi.api.domain.speech.dto.response.SpeechCreateRes;
import com.sisibibi.api.domain.speech.dto.response.SpeechCursorPageRes;
import com.sisibibi.api.domain.speech.dto.response.SpeechDetailRes;
import com.sisibibi.api.domain.speech.dto.response.SpeechImageUploadUrlRes;
import com.sisibibi.api.domain.speech.service.SpeechService;
import com.sisibibi.api.global.response.ApiResponse;
import com.sisibibi.api.global.security.AuthPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "의견", description = "토론방 의견 작성, 조회, 수정, 삭제 API")
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class SpeechController {

    private final SpeechService speechService;

    @Operation(
        summary = "메인 의견 작성",
        description = "현재 로그인 사용자가 지정한 토론방에 메인 의견을 작성합니다."
    )
    @PostMapping("/rooms/{roomId}/speeches")
    public ResponseEntity<ApiResponse<SpeechCreateRes>> createMainOpinion(
            @PathVariable @Positive Long roomId,
            @AuthenticationPrincipal AuthPrincipal principal,
            @Valid @RequestBody SpeechCreateReq request
    ) {
        SpeechCreateRes response = speechService.createMainOpinion(
                roomId,
                principal.userId(),
                request.toCommand()
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.created("메인 의견 작성이 완료되었습니다.", response));
    }

    @Operation(
        summary = "의견 목록 조회",
        description = "지정한 토론방의 의견 목록을 커서 기반으로 조회합니다."
    )
    @GetMapping("/rooms/{roomId}/speeches")
    public ResponseEntity<ApiResponse<SpeechCursorPageRes>> getSpeeches(
            @PathVariable @Positive Long roomId,
            @AuthenticationPrincipal AuthPrincipal principal,
            @RequestParam(required = false) @Positive Long cursor,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                "의견 목록 조회가 완료되었습니다.",
                speechService.getSpeeches(roomId, principal.userId(), cursor, size)
        ));
    }

    @Operation(
        summary = "의견 상세 조회",
        description = "지정한 의견의 상세 정보를 조회합니다."
    )
    @GetMapping("/speeches/{speechId}")
    public ResponseEntity<ApiResponse<SpeechDetailRes>> getSpeech(
            @PathVariable @Positive Long speechId,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                "의견 상세 조회가 완료되었습니다.",
                speechService.getSpeech(speechId, principal.userId())
        ));
    }

    @Operation(
        summary = "의견 수정",
        description = "현재 로그인 사용자가 자신이 작성한 의견을 수정합니다."
    )
    @PatchMapping("/speeches/{speechId}")
    public ResponseEntity<ApiResponse<SpeechDetailRes>> updateSpeech(
            @PathVariable @Positive Long speechId,
            @AuthenticationPrincipal AuthPrincipal principal,
            @Valid @RequestBody SpeechUpdateReq request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                "내 의견 수정이 완료되었습니다.",
                speechService.updateSpeech(speechId, principal.userId(), request.toCommand())
        ));
    }

    @Operation(
        summary = "의견 삭제",
        description = "현재 로그인 사용자가 자신이 작성한 의견을 삭제합니다."
    )
    @DeleteMapping("/speeches/{speechId}")
    public ResponseEntity<ApiResponse<Void>> deleteSpeech(
            @PathVariable @Positive Long speechId,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        speechService.deleteSpeech(speechId, principal.userId());

        return ResponseEntity.ok(ApiResponse.okMessage("내 의견 삭제가 완료되었습니다."));
    }

    @Operation(
        summary = "의견 이미지 업로드 URL 발급",
        description = "의견에 첨부할 이미지 파일을 업로드하기 위한 presigned URL을 발급합니다."
    )
    @PostMapping("/speeches/{speechId}/image-upload-url")
    public ResponseEntity<ApiResponse<SpeechImageUploadUrlRes>> createSpeechImageUploadUrl(
        @PathVariable @Positive Long speechId,
        @AuthenticationPrincipal AuthPrincipal principal,
        @Valid @RequestBody SpeechImageUploadUrlReq request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
            "이미지 업로드 URL 발급이 완료되었습니다.",
            speechService.createSpeechImageUploadUrl(
                speechId,
                principal.userId(),
                request.contentType(),
                request.fileSize()
            )
        ));
    }

    @Operation(
        summary = "의견 이미지 첨부 확정",
        description = "업로드가 완료된 이미지 키를 의견에 연결합니다."
    )
    @PatchMapping("/speeches/{speechId}/image")
    public ResponseEntity<ApiResponse<SpeechDetailRes>> confirmSpeechImage(
        @PathVariable @Positive Long speechId,
        @AuthenticationPrincipal AuthPrincipal principal,
        @Valid @RequestBody SpeechImageConfirmReq request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
            "의견 이미지 첨부가 완료되었습니다.",
            speechService.confirmSpeechImage(
                speechId,
                principal.userId(),
                request.imageKey()
            )
        ));
    }

    @Operation(
        summary = "의견 근거 링크 첨부",
        description = "의견에 근거 링크 URL을 첨부하거나 수정합니다."
    )
    @PatchMapping("/speeches/{speechId}/link")
    public ResponseEntity<ApiResponse<SpeechDetailRes>> updateSpeechLink(
            @PathVariable @Positive Long speechId,
            @AuthenticationPrincipal AuthPrincipal principal,
            @Valid @RequestBody SpeechLinkUpdateReq request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                "근거 링크 첨부가 완료되었습니다.",
                speechService.updateSpeechLink(speechId, principal.userId(), request.linkUrl())
        ));
    }
}
