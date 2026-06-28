package com.sisibibi.api.domain.report.controller;

import com.sisibibi.api.domain.report.dto.response.AiReportPdfDownloadUrlRes;
import com.sisibibi.api.domain.report.dto.response.AiReportPdfStatusRes;
import com.sisibibi.api.domain.report.dto.response.AiReportStatusRes;
import com.sisibibi.api.domain.report.service.AiReportPdfCommandService;
import com.sisibibi.api.global.exception.CustomException;
import com.sisibibi.api.global.exception.ErrorCode;
import com.sisibibi.api.global.response.ApiResponse;
import com.sisibibi.api.global.security.AuthPrincipal;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/rooms/{roomId}/ai-report")
public class AiReportPdfController {

    private final AiReportPdfCommandService commandService;

    @GetMapping("/status")
    public ResponseEntity<ApiResponse<AiReportStatusRes>> getStatus(
            @PathVariable @Positive Long roomId,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        requireLogin(principal);
        return ResponseEntity.ok(ApiResponse.ok(
                "AI 리포트 진행 상태 조회가 완료되었습니다.",
                commandService.getStatus(roomId, principal.userId())
        ));
    }

    @PostMapping("/pdf")
    public ResponseEntity<ApiResponse<AiReportPdfStatusRes>> requestPdf(
            @PathVariable @Positive Long roomId,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        requireLogin(principal);
        return ResponseEntity.ok(ApiResponse.ok(
                "AI 리포트 PDF 생성 요청이 완료되었습니다.",
                commandService.requestPdf(roomId, principal.userId())
        ));
    }

    @GetMapping("/pdf-download-url")
    public ResponseEntity<ApiResponse<AiReportPdfDownloadUrlRes>> getDownloadUrl(
            @PathVariable @Positive Long roomId,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        requireLogin(principal);
        return ResponseEntity.ok(ApiResponse.ok(
                "AI 리포트 PDF 다운로드 URL 발급이 완료되었습니다.",
                commandService.createDownloadUrl(roomId, principal.userId())
        ));
    }

    private void requireLogin(AuthPrincipal principal) {
        if (principal == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }
    }
}
