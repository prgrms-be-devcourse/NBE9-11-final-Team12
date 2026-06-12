package com.sisibibi.api.domain.auth.dto.response;

import com.sisibibi.api.domain.user.entity.User;

public record SignupRes(
        Long userId,
        String email,
        String nickname
) {

    public static SignupRes from(User user) {
        return new SignupRes(user.getId(), user.getEmail(), user.getNickname());
    }
}
