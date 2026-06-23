package com.sisibibi.api.domain.report.client.dto;

import com.sisibibi.api.domain.report.prompt.CustomPromptCommand;

import java.util.List;

public record AiReportGenerateReq(
        AiReportRoomPayload room,
        AiReportTopicPayload topic,
        List<AiReportSpeechPayload> speeches,
        List<CustomPromptCommand> customPrompts
) {

    public AiReportGenerateReq(
            AiReportRoomPayload room,
            AiReportTopicPayload topic,
            List<AiReportSpeechPayload> speeches
    ) {
        this(room, topic, speeches, List.of());
    }

    public AiReportGenerateReq {
        speeches = speeches == null ? List.of() : List.copyOf(speeches);
        customPrompts = customPrompts == null ? List.of() : List.copyOf(customPrompts);
    }
}
