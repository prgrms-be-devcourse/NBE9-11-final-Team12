package com.sisibibi.api.domain.speech.dto.event;

import com.sisibibi.api.domain.speech.entity.StageSummary;

import java.time.LocalDateTime;
import java.util.List;

public record StageSummaryEventPayload(
        Long summaryId,
        Long roomId,
        String moderatorSummary,
        List<String> keyPoints,
        int speechCount,
        int completedSpeakerCount,
        LocalDateTime triggeredAt,
        LocalDateTime completedAt
) {

    public static StageSummaryEventPayload from(StageSummary summary) {
        return new StageSummaryEventPayload(
                summary.getId(),
                summary.getRoomId(),
                summary.getModeratorSummary(),
                summary.getKeyPoints(),
                summary.getSpeechCount(),
                summary.getCompletedSpeakerCount(),
                summary.getTriggeredAt(),
                summary.getCompletedAt()
        );
    }
}
