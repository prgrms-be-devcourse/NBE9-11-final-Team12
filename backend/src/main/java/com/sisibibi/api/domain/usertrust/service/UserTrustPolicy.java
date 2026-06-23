package com.sisibibi.api.domain.usertrust.service;

import com.sisibibi.api.domain.usertrust.entity.UserActivityLevel;
import com.sisibibi.api.domain.usertrust.entity.UserTrustLevel;
import org.springframework.stereotype.Component;

@Component
public class UserTrustPolicy {

    public static final String POLICY_VERSION = "v2";

    private static final int BASE_SCORE = 50;
    private static final int MAX_REACTION_SCORE = 30;
    private static final int MIN_SCORE = 0;
    private static final int MAX_SCORE = 80;
    private static final int NORMAL_TRUST_SCORE = 30;
    private static final int RELIABLE_TRUST_SCORE = 55;
    private static final int TRUSTED_TRUST_SCORE = 70;
    private static final int ACTIVE_ACTIVITY_SCORE = 3;
    private static final int CONTRIBUTOR_ACTIVITY_SCORE = 10;
    private static final int LEADER_ACTIVITY_SCORE = 30;

    public UserTrustCalculation calculate(
            long receivedReactionCount,
            long lowViolationCount,
            long mediumViolationCount,
            long highViolationCount,
            long criticalViolationCount,
            long completedSpeechCount,
            long participatedRoomCount
    ) {
        int positiveScore = (int) Math.min(receivedReactionCount, MAX_REACTION_SCORE);
        long rawPenaltyScore = lowViolationCount
                + mediumViolationCount * 2
                + highViolationCount * 4
                + criticalViolationCount * 8;
        int penaltyScore = (int) Math.min(rawPenaltyScore, Integer.MAX_VALUE);
        int score = Math.max(
                MIN_SCORE,
                Math.min(MAX_SCORE, BASE_SCORE + positiveScore - penaltyScore)
        );
        long activityScore = completedSpeechCount * 3 + participatedRoomCount;

        return new UserTrustCalculation(
                score,
                positiveScore,
                penaltyScore,
                trustLevel(score),
                activityLevel(activityScore)
        );
    }

    private UserTrustLevel trustLevel(int score) {
        if (score < NORMAL_TRUST_SCORE) {
            return UserTrustLevel.CAUTION;
        }
        if (score < RELIABLE_TRUST_SCORE) {
            return UserTrustLevel.NORMAL;
        }
        if (score < TRUSTED_TRUST_SCORE) {
            return UserTrustLevel.RELIABLE;
        }
        return UserTrustLevel.TRUSTED;
    }

    private UserActivityLevel activityLevel(long activityScore) {
        if (activityScore < ACTIVE_ACTIVITY_SCORE) {
            return UserActivityLevel.NEW;
        }
        if (activityScore < CONTRIBUTOR_ACTIVITY_SCORE) {
            return UserActivityLevel.ACTIVE;
        }
        if (activityScore < LEADER_ACTIVITY_SCORE) {
            return UserActivityLevel.CONTRIBUTOR;
        }
        return UserActivityLevel.LEADER;
    }
}
