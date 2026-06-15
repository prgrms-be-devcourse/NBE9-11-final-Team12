package com.sisibibi.api.domain.speech.report.dto.command;

import com.sisibibi.api.domain.speech.report.entity.SpeechReportReason;

public record SpeechReportCreateCommand(
        SpeechReportReason reason,
        String description
) {
}
