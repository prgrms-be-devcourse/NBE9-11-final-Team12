package com.sisibibi.api.domain.speechreport.dto.request;

import com.sisibibi.api.domain.speechreport.entity.SpeechReportReviewAction;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SpeechReportReviewReq(
        @NotNull(message = "신고 처리 액션은 필수입니다.")
        SpeechReportReviewAction action,

        @Size(max = 500, message = "신고 처리 사유는 500자 이하여야 합니다.")
        String resolutionNote
) {
}
