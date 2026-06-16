package com.sisibibi.api.domain.speech.dto.response;

import com.sisibibi.api.domain.speech.entity.SpeakingQueue;
import java.time.LocalDateTime;

public record StageCurrentSpeakerRes(
        boolean hasCurrentSpeaker,
        CurrentSpeaker currentSpeaker
) {

    public static StageCurrentSpeakerRes empty() {
        return new StageCurrentSpeakerRes(false, null);
    }

    public static StageCurrentSpeakerRes of(
            SpeakingQueue speakingQueue,
            String nickname
    ) {
        return new StageCurrentSpeakerRes(
                true,
                new CurrentSpeaker(
                        speakingQueue.getUserId(),
                        nickname,
                        speakingQueue.getQueueOrder(),
                        speakingQueue.getAssignedAt(),
                        speakingQueue.getExpiresAt()
                )
        );
    }

    public record CurrentSpeaker(
            Long userId,
            String nickname,
            Integer queueOrder,
            LocalDateTime assignedAt,
            LocalDateTime expiresAt
    ) {
    }
}
