package com.sisibibi.api.domain.speech.dto.response;

import com.sisibibi.api.domain.speech.entity.SpeakingQueue;
import com.sisibibi.api.domain.speech.entity.SpeakingQueueStatus;

import java.time.LocalDateTime;

public record StageRequestRes(
        Long id,
        SpeakingQueueStatus status,
        Long roomId,
        Long userId,
        int queueOrder,
        LocalDateTime requestedAt,
        LocalDateTime canceledAt
) {

    public static StageRequestRes from(SpeakingQueue speakingQueue) {
        return new StageRequestRes(
                speakingQueue.getId(),
                speakingQueue.getStatus(),
                speakingQueue.getRoomId(),
                speakingQueue.getUserId(),
                speakingQueue.getQueueOrder(),
                speakingQueue.getRequestedAt(),
                speakingQueue.getCanceledAt()
        );
    }
}
