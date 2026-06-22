package com.sisibibi.api.global.security.session;

import com.sisibibi.api.domain.user.entity.User;
import com.sisibibi.api.domain.user.repository.UserRepository;
import com.sisibibi.api.global.exception.CustomException;
import com.sisibibi.api.global.exception.ErrorCode;
import com.sisibibi.api.global.security.jwt.JwtTokenProvider.TokenClaims;
import com.sisibibi.api.global.security.jwt.JwtTokenProvider.TokenType;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class TokenSessionValidatorTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final TokenSessionValidator tokenSessionValidator =
            new TokenSessionValidator(userRepository);

    @Test
    void validate_acceptsCurrentTokenVersion() {
        User user = user(2L);
        given(userRepository.findById(1L)).willReturn(Optional.of(user));

        assertThatCode(() -> tokenSessionValidator.validate(claims(2L)))
                .doesNotThrowAnyException();
    }

    @Test
    void validate_rejectsOldTokenVersion() {
        User user = user(3L);
        given(userRepository.findById(1L)).willReturn(Optional.of(user));

        assertThatThrownBy(() -> tokenSessionValidator.validate(claims(2L)))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_TOKEN);
    }

    @Test
    void validate_rejectsBannedUser() {
        User user = user(2L);
        user.ban();
        given(userRepository.findById(1L)).willReturn(Optional.of(user));

        assertThatThrownBy(() -> tokenSessionValidator.validate(claims(2L)))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USER_BANNED);
    }

    private User user(Long tokenVersion) {
        User user = User.signup("user@example.com", "password", "user");
        ReflectionTestUtils.setField(user, "id", 1L);
        ReflectionTestUtils.setField(user, "tokenVersion", tokenVersion);
        return user;
    }

    private TokenClaims claims(Long tokenVersion) {
        return new TokenClaims(
                1L,
                "user@example.com",
                "USER",
                "token-id",
                TokenType.ACCESS,
                tokenVersion,
                Instant.now().plusSeconds(300)
        );
    }
}
