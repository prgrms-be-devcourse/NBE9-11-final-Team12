package com.sisibibi.api.domain.report.controller;

import com.sisibibi.api.domain.report.dto.request.AiReportGenerateReq;
import com.sisibibi.api.domain.report.dto.response.AiReportRes;
import com.sisibibi.api.domain.report.service.AiReportService;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "AI 리포트", description = "토론방 AI 리포트 조회 및 생성 API")
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/rooms/{roomId}/ai-report")
public class AiReportController {

    private final AiReportService aiReportService;

    @Operation(
        summary = "AI 리포트 조회",
        description = "지정한 토론방의 AI 리포트를 조회합니다. 로그인 사용자는 사용자 기준 권한과 상태를 함께 반영해 조회합니다."
    )
    @GetMapping
    public ResponseEntity<ApiResponse<AiReportRes>> getAiReport(
            @PathVariable @Positive Long roomId,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        if (principal == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }

        return ResponseEntity.ok(ApiResponse.ok(
                "AI 리포트 조회가 완료되었습니다.",
                aiReportService.getReport(roomId, principal.userId())
        ));
    }

    @Operation(
        summary = "AI 리포트 생성 요청",
        description = "지정한 토론방의 AI 리포트 생성을 요청합니다. 요청 본문이 없으면 기본 생성 옵션을 사용합니다."
    )
    @PostMapping
    public ResponseEntity<ApiResponse<AiReportRes>> generateAiReport(
            @PathVariable @Positive Long roomId,
            @AuthenticationPrincipal AuthPrincipal principal,
            @RequestBody(required = false) AiReportGenerateReq request
    ) {
        if (principal == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }

        AiReportGenerateReq finalRequest = (request == null) ? AiReportGenerateReq.empty() : request;

        return ResponseEntity.ok(ApiResponse.ok(
                "AI 리포트 생성 요청이 완료되었습니다.",
                aiReportService.generateReport(roomId, principal.userId(), finalRequest)
        ));
    }
}
