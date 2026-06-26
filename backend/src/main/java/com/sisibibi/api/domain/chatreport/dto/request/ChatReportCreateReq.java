package com.sisibibi.api.domain.chatreport.dto.request;

import com.sisibibi.api.domain.chatreport.entity.ChatReportReason;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ChatReportCreateReq(
        @NotNull(message = "신고 사유는 필수입니다.")
        ChatReportReason reason,

        @Size(max = 500, message = "신고 상세 설명은 500자 이하여야 합니다.")
        String description
) {
}
