package com.sisibibi.api.domain.speech.dto.response;

import com.sisibibi.api.domain.speech.entity.SpeakingQueue;
import com.sisibibi.api.domain.speech.entity.SpeakingQueueStatus;
import com.sisibibi.api.domain.speech.entity.SpeechStance;

import java.time.LocalDateTime;

public record StageRequestRes(
        SpeakingQueueStatus status,
        Long roomId,
        Long userId,
        SpeechStance stance,
        int queueOrder,
        LocalDateTime requestedAt
) {

    public static StageRequestRes from(SpeakingQueue speakingQueue) {
        return new StageRequestRes(
                speakingQueue.getStatus(),
                speakingQueue.getRoomId(),
                speakingQueue.getUserId(),
                speakingQueue.getStance(),
                speakingQueue.getQueueOrder(),
                speakingQueue.getRequestedAt()
        );
    }
}
