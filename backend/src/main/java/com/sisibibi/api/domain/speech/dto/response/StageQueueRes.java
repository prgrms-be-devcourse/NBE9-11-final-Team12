package com.sisibibi.api.domain.speech.dto.response;

import com.sisibibi.api.domain.speech.entity.SpeakingQueue;
import com.sisibibi.api.domain.speech.entity.SpeakingQueueStatus;

import java.time.LocalDateTime;
import java.util.List;

public record StageQueueRes(
        Long roomId,
        List<StageQueueItemRes> items
) {

    public static StageQueueRes of(Long roomId, List<SpeakingQueue> speakingQueues) {
        return new StageQueueRes(
                roomId,
                speakingQueues.stream()
                        .map(StageQueueItemRes::from)
                        .toList()
        );
    }

    public record StageQueueItemRes(
            Long id,
            SpeakingQueueStatus status,
            Long userId,
            int queueOrder,
            LocalDateTime requestedAt
    ) {

        public static StageQueueItemRes from(SpeakingQueue speakingQueue) {
            return new StageQueueItemRes(
                    speakingQueue.getId(),
                    speakingQueue.getStatus(),
                    speakingQueue.getUserId(),
                    speakingQueue.getQueueOrder(),
                    speakingQueue.getRequestedAt()
            );
        }
    }
}
