package com.sisibibi.api.domain.usersanction.dto.event;

import com.sisibibi.api.domain.usersanction.entity.UserSanction;
import com.sisibibi.api.domain.usersanction.entity.UserSanctionState;
import com.sisibibi.api.domain.usersanction.entity.UserSanctionType;

import java.time.LocalDateTime;

public record UserSanctionEventPayload(
        Long sanctionId,
        UserSanctionType type,
        String reason,
        UserSanctionState state,
        LocalDateTime startsAt,
        LocalDateTime endsAt
) {

    public static UserSanctionEventPayload from(
            UserSanction sanction,
            LocalDateTime now
    ) {
        return new UserSanctionEventPayload(
                sanction.getId(),
                sanction.getType(),
                sanction.getReason(),
                sanction.stateAt(now),
                sanction.getStartsAt(),
                sanction.getEndsAt()
        );
    }
}
