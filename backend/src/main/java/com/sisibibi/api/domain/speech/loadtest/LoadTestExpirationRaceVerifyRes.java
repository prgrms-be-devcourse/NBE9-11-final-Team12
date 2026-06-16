package com.sisibibi.api.domain.speech.loadtest;

public record LoadTestExpirationRaceVerifyRes(
        Long roomId,
        int terminalCount,
        int completedCount,
        int expiredCount,
        int assignedCount,
        int waitingCount,
        boolean valid
) {
}
