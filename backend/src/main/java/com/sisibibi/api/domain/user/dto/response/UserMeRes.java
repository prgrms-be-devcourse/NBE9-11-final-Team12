package com.sisibibi.api.domain.user.dto.response;

import com.sisibibi.api.domain.user.entity.User;
import com.sisibibi.api.domain.user.entity.UserRole;
import com.sisibibi.api.domain.user.entity.UserStatus;

public record UserMeRes(
        Long userId,
        String email,
        String nickname,
        UserRole role,
        UserStatus status
) {

    public static UserMeRes from(User user) {
        return new UserMeRes(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getRole(),
                user.getStatus()
        );
    }
}
