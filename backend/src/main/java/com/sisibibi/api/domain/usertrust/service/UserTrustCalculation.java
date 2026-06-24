package com.sisibibi.api.domain.usertrust.service;

import com.sisibibi.api.domain.usertrust.entity.UserActivityLevel;
import com.sisibibi.api.domain.usertrust.entity.UserTrustLevel;

public record UserTrustCalculation(
        int score,
        int positiveScore,
        int penaltyScore,
        UserTrustLevel trustLevel,
        UserActivityLevel activityLevel
) {
}
