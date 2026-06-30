package com.sisibibi.api.domain.report.controller;

import com.sisibibi.api.domain.report.dto.response.AiReportPdfDownloadUrlRes;
import com.sisibibi.api.domain.report.dto.response.AiReportPdfStatusRes;
import com.sisibibi.api.domain.report.dto.response.AiReportStatusRes;
import com.sisibibi.api.domain.report.entity.AiReportPdfType;
import com.sisibibi.api.domain.report.service.AiReportPdfCommandService;
import com.sisibibi.api.global.exception.CustomException;
import com.sisibibi.api.global.exception.ErrorCode;
import com.sisibibi.api.global.response.ApiResponse;
import com.sisibibi.api.global.security.AuthPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "AI 리포트 PDF", description = "AI 리포트 PDF 생성 요청, 상태 조회, 다운로드 URL 발급 API")
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/rooms/{roomId}/ai-report")
public class AiReportPdfController {

    private final AiReportPdfCommandService commandService;

    @Operation(
        summary = "AI 리포트 PDF 상태 조회",
        description = "현재 사용자의 AI 리포트 PDF 내보내기 상태를 조회합니다."
    )
    @GetMapping("/status")
    public ResponseEntity<ApiResponse<AiReportStatusRes>> getStatus(
            @PathVariable @Positive Long roomId,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        if (principal == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }
        return ResponseEntity.ok(ApiResponse.ok(
                "AI 리포트 진행 상태 조회가 완료되었습니다.",
                commandService.getStatus(roomId, principal.userId())
        ));
    }

    @Operation(
        summary = "AI 리포트 PDF 생성 요청",
        description = "AI 리포트가 완료된 토론방에 대해 PDF 생성을 요청합니다."
    )
    @PostMapping("/pdf")
    public ResponseEntity<ApiResponse<AiReportPdfStatusRes>> requestPdf(
            @PathVariable @Positive Long roomId,
            @RequestParam(defaultValue = "BASE") AiReportPdfType pdfType,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        if (principal == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }
        return ResponseEntity.ok(ApiResponse.ok(
                "AI 리포트 PDF 생성 요청이 완료되었습니다.",
                commandService.requestPdf(roomId, principal.userId(), pdfType)
        ));
    }

    @Operation(
        summary = "AI 리포트 PDF 다운로드 URL 발급",
        description = "PDF 생성이 완료된 경우 Presigned 다운로드 URL을 발급합니다."
    )
    @GetMapping("/pdf-download-url")
    public ResponseEntity<ApiResponse<AiReportPdfDownloadUrlRes>> getDownloadUrl(
            @PathVariable @Positive Long roomId,
            @RequestParam(defaultValue = "BASE") AiReportPdfType pdfType,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        if (principal == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }
        return ResponseEntity.ok(ApiResponse.ok(
                "AI 리포트 PDF 다운로드 URL 발급이 완료되었습니다.",
                commandService.createDownloadUrl(roomId, principal.userId(), pdfType)
        ));
    }
}
