package com.sisibibi.api.domain.usertrust.dto.response;

import com.sisibibi.api.domain.usertrust.entity.UserActivityLevel;
import com.sisibibi.api.domain.usertrust.entity.UserTrustLevel;

import java.time.LocalDateTime;

public record UserTrustSummaryRes(
        Long userId,
        String nickname,
        int score,
        UserTrustLevel trustLevel,
        UserActivityLevel activityLevel,
        String policyVersion,
        LocalDateTime calculatedAt
) {
}
