package com.sisibibi.api.domain.speech.dto.response;

import com.sisibibi.api.domain.speech.entity.SpeechStance;
import java.util.List;

public record StageQueueRes(
        long totalWaitingCount,
        int offset,
        int size,
        boolean hasNext,
        List<WaitingSpeaker> items
) {

    public static StageQueueRes of(
            long totalWaitingCount,
            int offset,
            int size,
            List<WaitingSpeaker> items
    ) {
        boolean hasNext = offset + items.size() < totalWaitingCount;
        return new StageQueueRes(totalWaitingCount, offset, size, hasNext, items);
    }

    public record WaitingSpeaker(
            int rank,
            Long userId,
            String nickname,
            SpeechStance stance
    ) {
    }
}
