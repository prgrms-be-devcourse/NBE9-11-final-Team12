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
        assertThat(result.activityLevel()).isEqualTo(UserActivityLevel.LEADER);
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

    @Test
    void calculate_assignsNormalToBaseScore() {
        UserTrustCalculation result = policy.calculate(
                0,
                0,
                0,
                0,
                0,
                0,
                0
        );

        assertThat(result.score()).isEqualTo(50);
        assertThat(result.trustLevel()).isEqualTo(UserTrustLevel.NORMAL);
        assertThat(result.activityLevel()).isEqualTo(UserActivityLevel.NEW);
    }

    @Test
    void calculate_assignsTrustLevelsAtAdjustedBoundaries() {
        assertThat(calculateTrustLevel(29)).isEqualTo(UserTrustLevel.CAUTION);
        assertThat(calculateTrustLevel(30)).isEqualTo(UserTrustLevel.NORMAL);
        assertThat(calculateTrustLevel(54)).isEqualTo(UserTrustLevel.NORMAL);
        assertThat(calculateTrustLevel(55)).isEqualTo(UserTrustLevel.RELIABLE);
        assertThat(calculateTrustLevel(69)).isEqualTo(UserTrustLevel.RELIABLE);
        assertThat(calculateTrustLevel(70)).isEqualTo(UserTrustLevel.TRUSTED);
        assertThat(calculateTrustLevel(80)).isEqualTo(UserTrustLevel.TRUSTED);
    }

    @Test
    void calculate_assignsActivityLevelsAtBoundaries() {
        assertThat(calculateActivityLevel(0, 2)).isEqualTo(UserActivityLevel.NEW);
        assertThat(calculateActivityLevel(1, 0)).isEqualTo(UserActivityLevel.ACTIVE);
        assertThat(calculateActivityLevel(3, 1)).isEqualTo(UserActivityLevel.CONTRIBUTOR);
        assertThat(calculateActivityLevel(10, 0)).isEqualTo(UserActivityLevel.LEADER);
    }

    private UserTrustLevel calculateTrustLevel(int targetScore) {
        int scoreDifference = targetScore - 50;
        long reactionCount = Math.max(scoreDifference, 0);
        long lowViolationCount = Math.max(-scoreDifference, 0);

        return policy.calculate(
                reactionCount,
                lowViolationCount,
                0,
                0,
                0,
                0,
                0
        ).trustLevel();
    }

    private UserActivityLevel calculateActivityLevel(long completedSpeechCount, long participatedRoomCount) {
        return policy.calculate(
                0,
                0,
                0,
                0,
                0,
                completedSpeechCount,
                participatedRoomCount
        ).activityLevel();
    }
}
