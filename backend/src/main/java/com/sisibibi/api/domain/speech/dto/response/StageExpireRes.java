package com.sisibibi.api.domain.speech.dto.response;

import com.sisibibi.api.domain.speech.entity.SpeakingQueue;

public record StageExpireRes(
        Long roomId,
        StageRequestRes expiredSpeaker,
        CurrentSpeakerRes nextSpeaker
) {

    public static StageExpireRes of(
            Long roomId,
            SpeakingQueue expiredSpeaker,
            SpeakingQueue nextSpeaker
    ) {
        return new StageExpireRes(
                roomId,
                StageRequestRes.from(expiredSpeaker),
                nextSpeaker == null ? null : CurrentSpeakerRes.from(nextSpeaker)
        );
    }
}
