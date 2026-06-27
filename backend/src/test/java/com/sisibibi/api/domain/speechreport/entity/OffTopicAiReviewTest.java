package com.sisibibi.api.domain.speechreport.entity;

import com.sisibibi.api.domain.speechreport.service.OffTopicAiReviewResult;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OffTopicAiReviewTest {

    @Test
    void complete_changesPendingReviewToCompleted() {
        OffTopicAiReview review = pendingReview();

        review.complete(
                new OffTopicAiReviewResult(true, "논점 이탈입니다.", 0.9),
                LocalDateTime.of(2026, 6, 27, 3, 30)
        );

        assertThat(review.getStatus()).isEqualTo(OffTopicAiReviewStatus.COMPLETED);
        assertThat(review.isOffTopic()).isTrue();
        assertThat(review.getReason()).isEqualTo("논점 이탈입니다.");
        assertThat(review.getConfidence()).isEqualTo(0.9);
        assertThat(review.getCompletedAt())
                .isEqualTo(LocalDateTime.of(2026, 6, 27, 3, 30));
    }

    @Test
    void complete_throwsWhenReviewIsAlreadyCompleted() {
        OffTopicAiReview review = pendingReview();
        review.complete(
                new OffTopicAiReviewResult(true, "논점 이탈입니다.", 0.9),
                LocalDateTime.of(2026, 6, 27, 3, 30)
        );

        assertThatThrownBy(() -> review.complete(
                new OffTopicAiReviewResult(false, "두 번째 결과입니다.", 0.2),
                LocalDateTime.of(2026, 6, 27, 3, 31)
        ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("이미 처리된 AI 리뷰입니다.");
    }

    @Test
    void fail_throwsWhenReviewIsAlreadyCompleted() {
        OffTopicAiReview review = pendingReview();
        review.complete(
                new OffTopicAiReviewResult(true, "논점 이탈입니다.", 0.9),
                LocalDateTime.of(2026, 6, 27, 3, 30)
        );

        assertThatThrownBy(() -> review.fail("timeout"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("이미 처리된 AI 리뷰입니다.");
    }

    private OffTopicAiReview pendingReview() {
        return OffTopicAiReview.pending(
                10L,
                1L,
                "신고된 의견",
                5,
                5,
                50
        );
    }
}
