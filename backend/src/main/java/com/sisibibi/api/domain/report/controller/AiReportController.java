package com.sisibibi.api.domain.report.controller;

import com.sisibibi.api.domain.report.dto.response.AiReportRes;
import com.sisibibi.api.domain.report.service.AiReportService;
import com.sisibibi.api.global.response.ApiResponse;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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
public class AiReportController {

    private final AiReportService aiReportService;

    @GetMapping
    public ResponseEntity<ApiResponse<AiReportRes>> getAiReport(
            @PathVariable @Positive Long roomId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                "AI 리포트 조회가 완료되었습니다.",
                aiReportService.getReport(roomId)
        ));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AiReportRes>> generateAiReport(
            @PathVariable @Positive Long roomId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                "AI 리포트 생성 요청이 완료되었습니다.",
                aiReportService.generateReport(roomId)
        ));
    }
}
