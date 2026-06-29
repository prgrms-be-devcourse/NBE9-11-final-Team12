package com.sisibibi.api.domain.speech.dto.event;

import com.sisibibi.api.domain.speech.entity.SpeakingQueue;
import com.sisibibi.api.domain.speech.entity.SpeakingQueueStatus;
import com.sisibibi.api.domain.speech.entity.SpeechStance;
import java.time.LocalDateTime;

public record StageEventPayload(
        Long roomId,
        Long userId,
        SpeechStance stance,
        Integer queueOrder,
        SpeakingQueueStatus status,
        LocalDateTime assignedAt,
        LocalDateTime expiresAt,
        StageTurnEndReason endReason,
        boolean balancedAssignment,
        LocalDateTime occurredAt
) {

    public static StageEventPayload from(SpeakingQueue speakingQueue) {
        return from(speakingQueue, null, false);
    }

    public static StageEventPayload from(
            SpeakingQueue speakingQueue,
            StageTurnEndReason endReason
    ) {
        return from(speakingQueue, endReason, false);
    }

    public static StageEventPayload from(
            SpeakingQueue speakingQueue,
            StageTurnEndReason endReason,
            boolean balancedAssignment
    ) {
        return new StageEventPayload(
                speakingQueue.getRoomId(),
                speakingQueue.getUserId(),
                speakingQueue.getStance(),
                speakingQueue.getQueueOrder(),
                speakingQueue.getStatus(),
                speakingQueue.getAssignedAt(),
                speakingQueue.getExpiresAt(),
                endReason,
                balancedAssignment,
                LocalDateTime.now()
        );
    }
}
