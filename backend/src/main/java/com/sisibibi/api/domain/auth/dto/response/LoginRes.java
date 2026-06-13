package com.sisibibi.api.domain.auth.dto.response;

import com.sisibibi.api.domain.user.entity.User;

public record LoginRes(
        Long userId,
        String email,
        String nickname
) {

    public static LoginRes from(User user) {
        return new LoginRes(user.getId(), user.getEmail(), user.getNickname());
    }
}
