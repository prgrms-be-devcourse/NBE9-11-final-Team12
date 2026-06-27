package com.sisibibi.api.domain.user.dto.response;

import com.sisibibi.api.domain.user.entity.User;
import com.sisibibi.api.domain.user.entity.UserRole;
import com.sisibibi.api.domain.user.entity.UserStatus;

import java.time.LocalDateTime;

public record AdminUserRes(
        Long userId,
        String email,
        String nickname,
        UserRole role,
        UserStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static AdminUserRes from(User user) {
        return new AdminUserRes(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getRole(),
                user.getStatus(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
