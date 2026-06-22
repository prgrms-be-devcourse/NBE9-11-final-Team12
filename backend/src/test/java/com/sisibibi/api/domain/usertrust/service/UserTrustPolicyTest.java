package com.sisibibi.api.domain.usertrust.service;

import com.sisibibi.api.domain.usertrust.entity.UserActivityLevel;
import com.sisibibi.api.domain.usertrust.entity.UserTrustLevel;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserTrustPolicyTest {

    private final UserTrustPolicy policy = new UserTrustPolicy();

    @Test
    void calculate_appliesReactionRewardAndViolationPenalty() {
        UserTrustCalculation result = policy.calculate(
                18,
                2,
                1,
                1,
                0,
                4,
                3
        );

        assertThat(result.score()).isEqualTo(60);
        assertThat(result.trustLevel()).isEqualTo(UserTrustLevel.RELIABLE);
        assertThat(result.activityLevel()).isEqualTo(UserActivityLevel.CONTRIBUTOR);
    }

    @Test
    void calculate_capsReactionRewardAndClampsScore() {
        UserTrustCalculation result = policy.calculate(
                100,
                0,
                0,
                0,
                0,
                20,
                10
        );

        assertThat(result.score()).isEqualTo(80);
        assertThat(result.positiveScore()).isEqualTo(30);
        assertThat(result.trustLevel()).isEqualTo(UserTrustLevel.TRUSTED);
        assertThat(result.activityLevel()).isEqualTo(UserActivityLevel.TRUSTED);
    }

    @Test
    void calculate_doesNotAllowNegativeScore() {
        UserTrustCalculation result = policy.calculate(
                0,
                10,
                5,
                5,
                5,
                0,
                0
        );

        assertThat(result.score()).isZero();
        assertThat(result.trustLevel()).isEqualTo(UserTrustLevel.CAUTION);
    }
}
