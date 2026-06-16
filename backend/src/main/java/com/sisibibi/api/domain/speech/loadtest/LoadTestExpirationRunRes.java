package com.sisibibi.api.domain.speech.loadtest;

public record LoadTestExpirationRunRes(
        int candidateRoomCount,
        int expiredCount,
        int failureCount,
        long elapsedMs,
        long avgPerRoomMs
) {
}
