package com.sisibibi.api.domain.usertrust.service;

import com.sisibibi.api.domain.usertrust.entity.UserActivityLevel;
import com.sisibibi.api.domain.usertrust.entity.UserTrustLevel;
import org.springframework.stereotype.Component;

@Component
public class UserTrustPolicy {

    public static final String POLICY_VERSION = "v1";

    private static final int BASE_SCORE = 50;
    private static final int MAX_REACTION_SCORE = 30;
    private static final int MIN_SCORE = 0;
    private static final int MAX_SCORE = 100;

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
        if (score < 30) {
            return UserTrustLevel.CAUTION;
        }
        if (score < 60) {
            return UserTrustLevel.NORMAL;
        }
        if (score < 80) {
            return UserTrustLevel.RELIABLE;
        }
        return UserTrustLevel.TRUSTED;
    }

    private UserActivityLevel activityLevel(long activityScore) {
        if (activityScore < 3) {
            return UserActivityLevel.NEW;
        }
        if (activityScore < 10) {
            return UserActivityLevel.ACTIVE;
        }
        if (activityScore < 30) {
            return UserActivityLevel.CONTRIBUTOR;
        }
        return UserActivityLevel.TRUSTED;
    }
}
