package com.sisibibi.api.domain.auth.dto.response;

import com.sisibibi.api.domain.user.entity.User;

public record TokenReissueRes(
        Long userId,
        String email,
        String nickname
) {

    public static TokenReissueRes from(User user) {
        return new TokenReissueRes(user.getId(), user.getEmail(), user.getNickname());
    }
}
