package com.sisibibi.api.domain.usersanction.service;

import com.sisibibi.api.domain.speechreport.entity.ViolationSeverity;
import com.sisibibi.api.domain.usersanction.entity.UserSanctionType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserSanctionRecommendationPolicyTest {

    private final UserSanctionRecommendationPolicy policy =
            new UserSanctionRecommendationPolicy();

    @Test
    void recommend_returnsWarning_forFirstLowViolation() {
        SanctionRecommendation recommendation = policy.recommend(
                ViolationSeverity.LOW,
                new ViolationHistorySummary(1, 0, 0, 0)
        );

        assertThat(recommendation.type()).isEqualTo(UserSanctionType.WARNING);
        assertThat(recommendation.durationHours()).isNull();
        assertThat(recommendation.accountSuspensionReviewRecommended()).isFalse();
    }

    @Test
    void recommend_returnsOneDayRestriction_whenAccumulatedScoreReachesFour() {
        SanctionRecommendation recommendation = policy.recommend(
                ViolationSeverity.MEDIUM,
                new ViolationHistorySummary(0, 2, 0, 0)
        );

        assertThat(recommendation.type())
                .isEqualTo(UserSanctionType.STAGE_RESTRICTION);
        assertThat(recommendation.durationHours()).isEqualTo(24);
        assertThat(recommendation.accountSuspensionReviewRecommended()).isFalse();
    }

    @Test
    void recommend_returnsSevenDayRestriction_forHighViolation() {
        SanctionRecommendation recommendation = policy.recommend(
                ViolationSeverity.HIGH,
                new ViolationHistorySummary(0, 0, 1, 0)
        );

        assertThat(recommendation.durationHours()).isEqualTo(168);
        assertThat(recommendation.accountSuspensionReviewRecommended()).isFalse();
    }

    @Test
    void recommend_returnsThirtyDayRestriction_forCriticalViolation() {
        SanctionRecommendation recommendation = policy.recommend(
                ViolationSeverity.CRITICAL,
                new ViolationHistorySummary(0, 0, 0, 1)
        );

        assertThat(recommendation.durationHours()).isEqualTo(720);
        assertThat(recommendation.accountSuspensionReviewRecommended()).isFalse();
    }

    @Test
    void recommend_returnsSevenDayRestriction_whenAccumulatedScoreReachesEight() {
        SanctionRecommendation recommendation = policy.recommend(
                ViolationSeverity.MEDIUM,
                new ViolationHistorySummary(0, 4, 0, 0)
        );

        assertThat(recommendation.durationHours()).isEqualTo(168);
        assertThat(recommendation.accountSuspensionReviewRecommended()).isFalse();
    }

    @Test
    void recommend_marksAccountSuspensionReview_whenAccumulatedScoreReachesTwentyFour() {
        SanctionRecommendation recommendation = policy.recommend(
                ViolationSeverity.MEDIUM,
                new ViolationHistorySummary(0, 12, 0, 0)
        );

        assertThat(recommendation.type())
                .isEqualTo(UserSanctionType.STAGE_RESTRICTION);
        assertThat(recommendation.durationHours()).isEqualTo(168);
        assertThat(recommendation.accountSuspensionReviewRecommended()).isTrue();
    }

    @Test
    void recommend_marksAccountSuspensionReview_whenCriticalViolationRepeated() {
        SanctionRecommendation recommendation = policy.recommend(
                ViolationSeverity.CRITICAL,
                new ViolationHistorySummary(0, 0, 0, 2)
        );

        assertThat(recommendation.type())
                .isEqualTo(UserSanctionType.STAGE_RESTRICTION);
        assertThat(recommendation.durationHours()).isEqualTo(720);
        assertThat(recommendation.accountSuspensionReviewRecommended()).isTrue();
    }
}
