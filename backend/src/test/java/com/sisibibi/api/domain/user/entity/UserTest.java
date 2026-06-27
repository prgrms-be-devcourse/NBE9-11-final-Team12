package com.sisibibi.api.domain.user.entity;

import com.sisibibi.api.global.exception.CustomException;
import com.sisibibi.api.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserTest {

    @Test
    void ban_changesStatusAndInvalidatesTokens() {
        User user = User.signup("user@example.com", "password", "user");

        user.ban();

        assertThat(user.getStatus()).isEqualTo(UserStatus.BANNED);
        assertThat(user.getTokenVersion()).isEqualTo(1L);
    }

    @Test
    void ban_throwsUserBanned_whenAlreadyBanned() {
        User user = User.signup("user@example.com", "password", "user");
        user.ban();

        assertThatThrownBy(user::ban)
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USER_BANNED);
    }
}
