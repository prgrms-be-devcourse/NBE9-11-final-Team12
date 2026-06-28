package com.sisibibi.api.domain.report.controller;

import com.sisibibi.api.domain.report.dto.request.AiReportWorkerCompleteReq;
import com.sisibibi.api.domain.report.dto.request.AiReportWorkerFailReq;
import com.sisibibi.api.domain.report.dto.request.AiReportWorkerProcessingReq;
import com.sisibibi.api.domain.report.dto.response.AiReportRes;
import com.sisibibi.api.domain.report.dto.response.AiReportWorkerProcessingRes;
import com.sisibibi.api.domain.report.service.AiReportWorkerAuthService;
import com.sisibibi.api.domain.report.service.AiReportWorkerCallbackService;
import com.sisibibi.api.global.response.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
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
@RequestMapping("/api/v1/internal/ai-reports/{reportId}")
public class AiReportWorkerCallbackController {

    private static final String WORKER_TOKEN_HEADER = "X-AI-Worker-Token";

    private final AiReportWorkerCallbackService callbackService;
    private final AiReportWorkerAuthService authService;

    @PostMapping("/processing")
    public ResponseEntity<ApiResponse<AiReportWorkerProcessingRes>> startProcessing(
            @PathVariable @Positive Long reportId,
            @RequestHeader(WORKER_TOKEN_HEADER) String workerToken,
            @Valid @RequestBody AiReportWorkerProcessingReq request
    ) {
        authService.validate(workerToken);
        return ResponseEntity.ok(ApiResponse.ok(
                "AI 리포트 작업 입력 데이터 조회가 완료되었습니다.",
                callbackService.startProcessing(reportId, request.generationType())
        ));
    }

    @PostMapping("/complete")
    public ResponseEntity<ApiResponse<AiReportRes>> complete(
            @PathVariable @Positive Long reportId,
            @RequestHeader(WORKER_TOKEN_HEADER) String workerToken,
            @Valid @RequestBody AiReportWorkerCompleteReq request
    ) {
        authService.validate(workerToken);
        return ResponseEntity.ok(ApiResponse.ok(
                "AI 리포트 생성 결과 저장이 완료되었습니다.",
                callbackService.complete(reportId, request)
        ));
    }

    @PostMapping("/fail")
    public ResponseEntity<ApiResponse<AiReportRes>> fail(
            @PathVariable @Positive Long reportId,
            @RequestHeader(WORKER_TOKEN_HEADER) String workerToken,
            @Valid @RequestBody AiReportWorkerFailReq request
    ) {
        authService.validate(workerToken);
        return ResponseEntity.ok(ApiResponse.ok(
                "AI 리포트 생성 실패 상태 저장이 완료되었습니다.",
                callbackService.fail(reportId, request)
        ));
    }
}
