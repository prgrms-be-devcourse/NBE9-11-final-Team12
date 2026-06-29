package com.sisibibi.api.domain.speech.service;

import com.sisibibi.api.domain.speech.entity.SpeakingQueue;
import java.util.List;
import java.util.Optional;

public record SpeakingQueueAssignmentResult(
        Optional<SpeakingQueue> assignedRequest,
        List<SpeakingQueue> canceledRequests,
        boolean balancedAssignment
) {

    public SpeakingQueueAssignmentResult {
        canceledRequests = List.copyOf(canceledRequests);
    }

    public static SpeakingQueueAssignmentResult empty() {
        return new SpeakingQueueAssignmentResult(Optional.empty(), List.of(), false);
    }

    public static SpeakingQueueAssignmentResult of(
            Optional<SpeakingQueue> assignedRequest,
            List<SpeakingQueue> canceledRequests
    ) {
        return of(assignedRequest, canceledRequests, false);
    }

    public static SpeakingQueueAssignmentResult of(
            Optional<SpeakingQueue> assignedRequest,
            List<SpeakingQueue> canceledRequests,
            boolean balancedAssignment
    ) {
        return new SpeakingQueueAssignmentResult(
                assignedRequest,
                canceledRequests,
                balancedAssignment
        );
    }
}
