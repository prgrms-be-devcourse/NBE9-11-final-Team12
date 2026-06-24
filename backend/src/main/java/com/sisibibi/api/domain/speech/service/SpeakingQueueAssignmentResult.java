package com.sisibibi.api.domain.speech.service;

import com.sisibibi.api.domain.speech.entity.SpeakingQueue;
import java.util.List;
import java.util.Optional;

public record SpeakingQueueAssignmentResult(
        Optional<SpeakingQueue> assignedRequest,
        List<SpeakingQueue> canceledRequests
) {

    public SpeakingQueueAssignmentResult {
        canceledRequests = List.copyOf(canceledRequests);
    }

    public static SpeakingQueueAssignmentResult empty() {
        return new SpeakingQueueAssignmentResult(Optional.empty(), List.of());
    }

    public static SpeakingQueueAssignmentResult of(
            Optional<SpeakingQueue> assignedRequest,
            List<SpeakingQueue> canceledRequests
    ) {
        return new SpeakingQueueAssignmentResult(assignedRequest, canceledRequests);
    }
}
