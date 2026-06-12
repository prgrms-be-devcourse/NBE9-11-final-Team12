package com.sisibibi.api.domain.speech.dto.response;

import com.sisibibi.api.domain.speech.entity.SpeakingQueue;
import com.sisibibi.api.domain.speech.entity.SpeakingQueueStatus;

import java.time.LocalDateTime;

public record CurrentSpeakerRes(
        Long id,
        SpeakingQueueStatus status,
        Long roomId,
        Long userId,
        int queueOrder,
        LocalDateTime requestedAt
) {

    public static CurrentSpeakerRes from(SpeakingQueue speakingQueue) {
        return new CurrentSpeakerRes(
                speakingQueue.getId(),
                speakingQueue.getStatus(),
                speakingQueue.getRoomId(),
                speakingQueue.getUserId(),
                speakingQueue.getQueueOrder(),
                speakingQueue.getRequestedAt()
        );
    }
}
