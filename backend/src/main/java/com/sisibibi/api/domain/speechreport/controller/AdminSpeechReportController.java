package com.sisibibi.api.domain.speechreport.controller;

import com.sisibibi.api.domain.speechreport.dto.response.SpeechReportDetailRes;
import com.sisibibi.api.domain.speechreport.dto.response.SpeechReportSummaryRes;
import com.sisibibi.api.domain.speechreport.entity.SpeechReportReason;
import com.sisibibi.api.domain.speechreport.entity.SpeechReportStatus;
import com.sisibibi.api.domain.speechreport.service.SpeechReportService;
import com.sisibibi.api.global.response.ApiResponse;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/reports")
public class AdminSpeechReportController {

    private final SpeechReportService speechReportService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<SpeechReportSummaryRes>>> getReports(
            @RequestParam(required = false) SpeechReportStatus status,
            @RequestParam(required = false) SpeechReportReason reason,
            @PageableDefault(
                    size = 20,
                    sort = {"createdAt", "id"},
                    direction = Sort.Direction.DESC
            ) Pageable pageable
    ) {
        Page<SpeechReportSummaryRes> response =
                speechReportService.getReports(status, reason, pageable);

        return ResponseEntity.ok(ApiResponse.ok("의견 신고 목록 조회가 완료되었습니다.", response));
    }

    @GetMapping("/{reportId}")
    public ResponseEntity<ApiResponse<SpeechReportDetailRes>> getReport(
            @PathVariable @Positive Long reportId
    ) {
        SpeechReportDetailRes response = speechReportService.getReport(reportId);

        return ResponseEntity.ok(ApiResponse.ok("의견 신고 상세 조회가 완료되었습니다.", response));
    }
}
