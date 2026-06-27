package com.sisibibi.api.domain.chatreport.dto.request;

import com.sisibibi.api.domain.chatreport.entity.ChatReportReviewAction;
import com.sisibibi.api.domain.chatreport.entity.ChatReportSeverity;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ChatReportReviewReq(
        @NotNull(message = "채팅 신고 처리 액션은 필수입니다.")
        ChatReportReviewAction action,

        @Size(max = 500, message = "채팅 신고 처리 사유는 500자 이하여야 합니다.")
        String resolutionNote,

        ChatReportSeverity severity
) {
}
