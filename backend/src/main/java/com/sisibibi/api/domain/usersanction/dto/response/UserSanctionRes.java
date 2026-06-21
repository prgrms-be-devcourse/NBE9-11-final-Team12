package com.sisibibi.api.domain.usersanction.dto.response;

import com.sisibibi.api.domain.usersanction.entity.UserSanction;
import com.sisibibi.api.domain.usersanction.entity.UserSanctionState;
import com.sisibibi.api.domain.usersanction.entity.UserSanctionType;

import java.time.LocalDateTime;

public record UserSanctionRes(
        Long sanctionId,
        Long userId,
        Long adminUserId,
        Long reportId,
        UserSanctionType type,
        String reason,
        UserSanctionState state,
        LocalDateTime startsAt,
        LocalDateTime endsAt,
        LocalDateTime revokedAt,
        Long revokedBy,
        String revocationReason,
        LocalDateTime createdAt
) {

    public static UserSanctionRes from(UserSanction sanction, LocalDateTime now) {
        return new UserSanctionRes(
                sanction.getId(),
                sanction.getUserId(),
                sanction.getAdminUserId(),
                sanction.getReportId(),
                sanction.getType(),
                sanction.getReason(),
                sanction.stateAt(now),
                sanction.getStartsAt(),
                sanction.getEndsAt(),
                sanction.getRevokedAt(),
                sanction.getRevokedBy(),
                sanction.getRevocationReason(),
                sanction.getCreatedAt()
        );
    }
}
