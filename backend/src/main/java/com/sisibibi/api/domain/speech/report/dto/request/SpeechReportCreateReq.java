package com.sisibibi.api.domain.speech.report.dto.request;

import com.sisibibi.api.domain.speech.report.dto.command.SpeechReportCreateCommand;
import com.sisibibi.api.domain.speech.report.entity.SpeechReportReason;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SpeechReportCreateReq(
        @NotNull(message = "신고 사유는 필수입니다.")
        SpeechReportReason reason,

        @Size(max = 500, message = "신고 상세 설명은 500자 이하여야 합니다.")
        String description
) {

    public SpeechReportCreateCommand toCommand() {
        return new SpeechReportCreateCommand(reason, description);
    }
}
