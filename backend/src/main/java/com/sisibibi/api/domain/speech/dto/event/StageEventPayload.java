package com.sisibibi.api.domain.speech.dto.event;

import com.sisibibi.api.domain.speech.entity.SpeakingQueue;
import com.sisibibi.api.domain.speech.entity.SpeakingQueueStatus;
import java.time.LocalDateTime;

public record StageEventPayload(
        Long roomId,
        Long userId,
        Integer queueOrder,
        SpeakingQueueStatus status,
        LocalDateTime assignedAt,
        LocalDateTime expiresAt,
        StageTurnEndReason endReason,
        LocalDateTime occurredAt
) {

    public static StageEventPayload from(SpeakingQueue speakingQueue) {
        return from(speakingQueue, null);
    }

    public static StageEventPayload from(
            SpeakingQueue speakingQueue,
            StageTurnEndReason endReason
    ) {
        return new StageEventPayload(
                speakingQueue.getRoomId(),
                speakingQueue.getUserId(),
                speakingQueue.getQueueOrder(),
                speakingQueue.getStatus(),
                speakingQueue.getAssignedAt(),
                speakingQueue.getExpiresAt(),
                endReason,
                LocalDateTime.now()
        );
    }
}
