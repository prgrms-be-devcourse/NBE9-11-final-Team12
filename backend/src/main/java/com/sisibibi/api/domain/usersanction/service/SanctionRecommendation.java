package com.sisibibi.api.domain.usersanction.service;

import com.sisibibi.api.domain.usersanction.entity.UserSanctionType;

public record SanctionRecommendation(
        UserSanctionType type,
        Integer durationHours,
        String reason
) {
}
