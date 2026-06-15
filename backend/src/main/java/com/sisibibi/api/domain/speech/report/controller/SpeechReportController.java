package com.sisibibi.api.domain.speech.report.controller;

import com.sisibibi.api.domain.speech.report.dto.request.SpeechReportCreateReq;
import com.sisibibi.api.domain.speech.report.dto.response.SpeechReportCreateRes;
import com.sisibibi.api.domain.speech.report.service.SpeechReportService;
import com.sisibibi.api.global.response.ApiResponse;
import com.sisibibi.api.global.security.AuthPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/speeches")
public class SpeechReportController {

    private final SpeechReportService speechReportService;

    @PostMapping("/{speechId}/reports")
    public ResponseEntity<ApiResponse<SpeechReportCreateRes>> createReport(
            @PathVariable @Positive Long speechId,
            @AuthenticationPrincipal AuthPrincipal principal,
            @Valid @RequestBody SpeechReportCreateReq request
    ) {
        SpeechReportCreateRes response = speechReportService.createReport(
                speechId,
                principal.userId(),
                request.toCommand()
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.created("의견 신고가 접수되었습니다.", response));
    }
}
