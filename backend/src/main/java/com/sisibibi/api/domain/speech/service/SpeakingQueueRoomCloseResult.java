package com.sisibibi.api.domain.speech.service;

import com.sisibibi.api.domain.speech.entity.SpeakingQueue;
import java.util.List;

public record SpeakingQueueRoomCloseResult(
        List<SpeakingQueue> canceledRequests,
        List<SpeakingQueue> completedRequests
) {

    public SpeakingQueueRoomCloseResult {
        canceledRequests = List.copyOf(canceledRequests);
        completedRequests = List.copyOf(completedRequests);
    }

    public static SpeakingQueueRoomCloseResult of(
            List<SpeakingQueue> canceledRequests,
            List<SpeakingQueue> completedRequests
    ) {
        return new SpeakingQueueRoomCloseResult(canceledRequests, completedRequests);
    }
}
