package com.sisibibi.api.domain.speech.dto.response;

import com.sisibibi.api.domain.speech.entity.SpeakingQueue;

public record StageCompleteRes(
        Long roomId,
        StageRequestRes completedSpeaker,
        CurrentSpeakerRes nextSpeaker
) {

    public static StageCompleteRes of(
            Long roomId,
            SpeakingQueue completedSpeaker,
            SpeakingQueue nextSpeaker
    ) {
        return new StageCompleteRes(
                roomId,
                StageRequestRes.from(completedSpeaker),
                nextSpeaker == null ? null : CurrentSpeakerRes.from(nextSpeaker)
        );
    }
}
