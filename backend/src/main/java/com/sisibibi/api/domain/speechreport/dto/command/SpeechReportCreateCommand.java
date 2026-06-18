package com.sisibibi.api.domain.speechreport.dto.command;

import com.sisibibi.api.domain.speechreport.entity.SpeechReportReason;

public record SpeechReportCreateCommand(
        SpeechReportReason reason,
        String description
) {
}
