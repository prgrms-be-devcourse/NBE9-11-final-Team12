package com.sisibibi.api.domain.speech.loadtest;

public record LoadTestExpirationRacePrepareRes(
        Long roomId,
        Long currentSpeakerUserId,
        Long nextSpeakerUserId,
        int preparedCurrentSpeakers,
        int preparedWaitingSpeakers
) {
}
