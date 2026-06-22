package com.sisibibi.api.domain.usertrust.dto.response;

import com.sisibibi.api.domain.usertrust.entity.UserActivityLevel;
import com.sisibibi.api.domain.usertrust.entity.UserTrustLevel;

public record UserTrustDetailRes(
        Long userId,
        String nickname,
        int score,
        UserTrustLevel trustLevel,
        UserActivityLevel activityLevel,
        long receivedReactionCount,
        long completedSpeechCount,
        long participatedRoomCount,
        long resolvedViolationCount,
        int positiveScore,
        int penaltyScore
) {
}
