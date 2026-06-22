package com.sisibibi.api.domain.usersanction.dto.response;

import com.sisibibi.api.domain.usersanction.entity.UserSanction;
import com.sisibibi.api.domain.usersanction.entity.UserSanctionType;

import java.time.LocalDateTime;

public record ActiveUserSanctionRes(
        Long sanctionId,
        UserSanctionType type,
        String reason,
        LocalDateTime startsAt,
        LocalDateTime endsAt
) {

    public static ActiveUserSanctionRes from(UserSanction sanction) {
        return new ActiveUserSanctionRes(
                sanction.getId(),
                sanction.getType(),
                sanction.getReason(),
                sanction.getStartsAt(),
                sanction.getEndsAt()
        );
    }
}
