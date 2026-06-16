package com.sisibibi.api.domain.speech.loadtest;

public record LoadTestExpirationPrepareRes(
        Long roomIdStart,
        Long userIdStart,
        int roomCount,
        int waitingPerRoom,
        int preparedCurrentSpeakers,
        int preparedWaitingSpeakers
) {
}
