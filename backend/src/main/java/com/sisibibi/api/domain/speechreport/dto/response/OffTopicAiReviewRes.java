package com.sisibibi.api.domain.speechreport.dto.response;

import com.sisibibi.api.domain.speechreport.entity.OffTopicAiReview;
import com.sisibibi.api.domain.speechreport.entity.OffTopicAiReviewStatus;

import java.time.LocalDateTime;

public record OffTopicAiReviewRes(
        Long reviewId,
        OffTopicAiReviewStatus status,
        Boolean offTopic,
        Double confidence,
        String reason,
        int reportCount,
        int threshold,
        int participantCount,
        LocalDateTime completedAt
) {

    public static OffTopicAiReviewRes from(OffTopicAiReview review) {
        if (review == null) {
            return null;
        }
        return new OffTopicAiReviewRes(
                review.getId(),
                review.getStatus(),
                review.getOffTopic(),
                review.getConfidence(),
                review.getReason(),
                review.getReportCount(),
                review.getThreshold(),
                review.getParticipantCount(),
                review.getCompletedAt()
        );
    }
}
