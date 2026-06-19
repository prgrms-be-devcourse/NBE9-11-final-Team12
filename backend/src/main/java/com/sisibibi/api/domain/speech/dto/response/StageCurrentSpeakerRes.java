package com.sisibibi.api.domain.speech.dto.response;

import com.sisibibi.api.domain.speech.entity.SpeechStance;
import com.sisibibi.api.domain.speech.repository.projection.CurrentSpeakerProjection;
import java.time.LocalDateTime;

public record StageCurrentSpeakerRes(
        boolean hasCurrentSpeaker,
        CurrentSpeaker currentSpeaker
) {

    public static StageCurrentSpeakerRes empty() {
        return new StageCurrentSpeakerRes(false, null);
    }

    public static StageCurrentSpeakerRes from(CurrentSpeakerProjection currentSpeaker) {
        return new StageCurrentSpeakerRes(
                true,
                new CurrentSpeaker(
                        currentSpeaker.getUserId(),
                        currentSpeaker.getNickname(),
                        currentSpeaker.getStance(),
                        currentSpeaker.getQueueOrder(),
                        currentSpeaker.getAssignedAt(),
                        currentSpeaker.getExpiresAt()
                )
        );
    }

    public record CurrentSpeaker(
            Long userId,
            String nickname,
            SpeechStance stance,
            Integer queueOrder,
            LocalDateTime assignedAt,
            LocalDateTime expiresAt
    ) {
    }
}
