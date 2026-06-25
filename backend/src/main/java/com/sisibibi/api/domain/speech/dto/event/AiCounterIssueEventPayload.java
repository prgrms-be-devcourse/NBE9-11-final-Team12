package com.sisibibi.api.domain.speech.dto.event;

import com.sisibibi.api.domain.speech.entity.AiCounterIssue;
import com.sisibibi.api.domain.speech.entity.SpeechStance;
import java.time.LocalDateTime;

public record AiCounterIssueEventPayload(
        Long issueId,
        Long roomId,
        Long triggerQueueId,
        SpeechStance targetStance,
        String content,
        LocalDateTime createdAt,
        LocalDateTime completedAt,
        LocalDateTime occurredAt
) {

    public static AiCounterIssueEventPayload from(AiCounterIssue issue) {
        return new AiCounterIssueEventPayload(
                issue.getId(),
                issue.getRoomId(),
                issue.getTriggerQueueId(),
                issue.getTargetStance(),
                issue.getContent(),
                issue.getCreatedAt(),
                issue.getCompletedAt(),
                LocalDateTime.now()
        );
    }
}
