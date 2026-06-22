package com.sisibibi.api.domain.report.client.dto;

import java.util.List;

public record AiReportGenerateReq(
        AiReportRoomPayload room,
        AiReportTopicPayload topic,
        List<AiReportSpeechPayload> speeches
) {
}
