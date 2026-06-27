package com.sisibibi.api.domain.speech.dto.response;

import com.sisibibi.api.domain.speech.entity.AiCounterIssue;
import com.sisibibi.api.domain.speech.entity.SpeechStance;
import java.time.LocalDateTime;

public record AiCounterIssueRes(
        Long issueId,
        Long roomId,
        Long triggerQueueId,
        SpeechStance targetStance,
        String content,
        LocalDateTime createdAt,
        LocalDateTime completedAt
) {

    public static AiCounterIssueRes from(AiCounterIssue issue) {
        return new AiCounterIssueRes(
                issue.getId(),
                issue.getRoomId(),
                issue.getTriggerQueueId(),
                issue.getTargetStance(),
                issue.getContent(),
                issue.getCreatedAt(),
                issue.getCompletedAt()
        );
    }
}
