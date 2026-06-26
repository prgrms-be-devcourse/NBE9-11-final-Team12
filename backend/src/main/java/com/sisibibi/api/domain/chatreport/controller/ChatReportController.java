package com.sisibibi.api.domain.chatreport.controller;

import com.sisibibi.api.domain.chatreport.dto.request.ChatReportCreateReq;
import com.sisibibi.api.domain.chatreport.dto.response.ChatReportCreateRes;
import com.sisibibi.api.domain.chatreport.service.ChatReportService;
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
@RequestMapping("/api/v1/rooms/{roomId}/chat/messages/{messageId}/reports")
public class ChatReportController {

    private final ChatReportService chatReportService;

    @PostMapping
    public ResponseEntity<ApiResponse<ChatReportCreateRes>> createReport(
            @PathVariable @Positive Long roomId,
            @PathVariable @Positive Long messageId,
            @AuthenticationPrincipal AuthPrincipal principal,
            @Valid @RequestBody ChatReportCreateReq request
    ) {
        ChatReportCreateRes response = chatReportService.createReport(
                roomId,
                messageId,
                principal.userId(),
                request
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.created("채팅 메시지 신고가 접수되었습니다.", response));
    }
}
