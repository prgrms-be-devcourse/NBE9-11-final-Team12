package com.sisibibi.api.domain.speech.dto.response;

import com.sisibibi.api.domain.speech.entity.StageSummary;
import java.time.LocalDateTime;
import java.util.List;

public record StageSummaryRes(
        Long summaryId,
        Long roomId,
        String status,
        String moderatorSummary,
        List<String> keyPoints,
        int speechCount,
        int completedSpeakerCount,
        LocalDateTime triggeredAt,
        LocalDateTime completedAt
) {

    public static StageSummaryRes from(StageSummary summary) {
        return new StageSummaryRes(
                summary.getId(),
                summary.getRoomId(),
                summary.getStatus().name(),
                summary.getModeratorSummary(),
                summary.getKeyPoints(),
                summary.getSpeechCount(),
                summary.getCompletedSpeakerCount(),
                summary.getTriggeredAt(),
                summary.getCompletedAt()
        );
    }
}
