package com.sisibibi.api.domain.usertrust.dto.response;

import com.sisibibi.api.domain.usertrust.entity.UserActivityLevel;
import com.sisibibi.api.domain.usertrust.entity.UserTrustLevel;

public record UserTrustSummaryRes(
        Long userId,
        String nickname,
        int score,
        UserTrustLevel trustLevel,
        UserActivityLevel activityLevel
) {
}
