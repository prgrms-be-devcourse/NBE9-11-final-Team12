package com.sisibibi.api.domain.usersanction.dto.response;

import com.sisibibi.api.domain.speechreport.entity.ViolationSeverity;
import com.sisibibi.api.domain.usersanction.entity.UserSanctionType;

public record UserSanctionRecommendationRes(
        Long reportId,
        Long userId,
        ViolationSeverity currentSeverity,
        int lookbackDays,
        long resolvedViolationCount,
        long lowCount,
        long mediumCount,
        long highCount,
        long criticalCount,
        int weightedScore,
        UserSanctionType recommendedType,
        Integer recommendedDurationHours,
        boolean activeSameTypeSanction,
        Long activeSameTypeSanctionId,
        java.time.LocalDateTime activeSameTypeEndsAt,
        String recommendationReason
) {
}
