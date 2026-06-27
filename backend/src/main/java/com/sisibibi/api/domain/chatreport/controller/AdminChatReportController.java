package com.sisibibi.api.domain.chatreport.controller;

import com.sisibibi.api.domain.chatreport.dto.request.ChatReportReviewReq;
import com.sisibibi.api.domain.chatreport.dto.response.ChatReportDetailRes;
import com.sisibibi.api.domain.chatreport.dto.response.ChatReportReviewRes;
import com.sisibibi.api.domain.chatreport.dto.response.ChatReportSummaryRes;
import com.sisibibi.api.domain.chatreport.entity.ChatReportReason;
import com.sisibibi.api.domain.chatreport.entity.ChatReportStatus;
import com.sisibibi.api.domain.chatreport.service.ChatReportService;
import com.sisibibi.api.global.response.ApiResponse;
import com.sisibibi.api.global.security.AuthPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/chat-reports")
public class AdminChatReportController {

    private final ChatReportService chatReportService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<ChatReportSummaryRes>>> getReports(
            @RequestParam(required = false) ChatReportStatus status,
            @RequestParam(required = false) ChatReportReason reason,
            @PageableDefault(
                    size = 20,
                    sort = {"createdAt", "id"},
                    direction = Sort.Direction.DESC
            ) Pageable pageable
    ) {
        Page<ChatReportSummaryRes> response =
                chatReportService.getReports(status, reason, pageable);

        return ResponseEntity.ok(ApiResponse.ok("채팅 신고 목록 조회가 완료되었습니다.", response));
    }

    @GetMapping("/{reportId}")
    public ResponseEntity<ApiResponse<ChatReportDetailRes>> getReport(
            @PathVariable @Positive Long reportId
    ) {
        ChatReportDetailRes response = chatReportService.getReport(reportId);

        return ResponseEntity.ok(ApiResponse.ok("채팅 신고 상세 조회가 완료되었습니다.", response));
    }

    @PatchMapping("/{reportId}")
    public ResponseEntity<ApiResponse<ChatReportReviewRes>> reviewReport(
            @PathVariable @Positive Long reportId,
            @AuthenticationPrincipal AuthPrincipal principal,
            @Valid @RequestBody ChatReportReviewReq request
    ) {
        ChatReportReviewRes response = chatReportService.reviewReport(
                reportId,
                principal.userId(),
                request.action(),
                request.resolutionNote(),
                request.severity()
        );

        return ResponseEntity.ok(ApiResponse.ok("채팅 신고 처리가 완료되었습니다.", response));
    }
}
