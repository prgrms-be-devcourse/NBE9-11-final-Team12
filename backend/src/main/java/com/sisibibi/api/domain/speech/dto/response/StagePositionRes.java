package com.sisibibi.api.domain.speech.dto.response;

import com.sisibibi.api.domain.speech.entity.SpeakingQueue;
import com.sisibibi.api.domain.speech.entity.SpeakingQueueStatus;

public record StagePositionRes(
        Long roomId,
        Long userId,
        SpeakingQueueStatus status,
        int queueOrder,
        long aheadCount,
        long waitingRank
) {

    public static StagePositionRes assigned(SpeakingQueue speakingQueue) {
        return new StagePositionRes(
                speakingQueue.getRoomId(),
                speakingQueue.getUserId(),
                speakingQueue.getStatus(),
                speakingQueue.getQueueOrder(),
                0,
                0
        );
    }

    public static StagePositionRes waiting(SpeakingQueue speakingQueue, long aheadCount) {
        return new StagePositionRes(
                speakingQueue.getRoomId(),
                speakingQueue.getUserId(),
                speakingQueue.getStatus(),
                speakingQueue.getQueueOrder(),
                aheadCount,
                aheadCount + 1
        );
    }
}
