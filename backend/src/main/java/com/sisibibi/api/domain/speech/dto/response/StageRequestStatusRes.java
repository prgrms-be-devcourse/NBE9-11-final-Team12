package com.sisibibi.api.domain.speech.dto.response;

import com.sisibibi.api.domain.speech.entity.SpeakingQueue;
import com.sisibibi.api.domain.speech.entity.SpeakingQueueStatus;
import java.time.LocalDateTime;

public record StageRequestStatusRes(
        boolean hasRequest,
        SpeakingQueueStatus status,
        Long roomId,
        Long userId,
        Integer queueOrder,
        Integer currentRank,
        boolean cancelable,
        LocalDateTime requestedAt,
        LocalDateTime assignedAt,
        LocalDateTime expiresAt
) {

    public static StageRequestStatusRes empty() {
        return new StageRequestStatusRes(
                false,
                null,
                null,
                null,
                null,
                null,
                false,
                null,
                null,
                null
        );
    }

    public static StageRequestStatusRes from(
            SpeakingQueue speakingQueue,
            Integer currentRank
    ) {
        return new StageRequestStatusRes(
                true,
                speakingQueue.getStatus(),
                speakingQueue.getRoomId(),
                speakingQueue.getUserId(),
                speakingQueue.getQueueOrder(),
                currentRank,
                speakingQueue.getStatus() == SpeakingQueueStatus.WAITING,
                speakingQueue.getRequestedAt(),
                speakingQueue.getAssignedAt(),
                speakingQueue.getExpiresAt()
        );
    }
}
