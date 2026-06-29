package com.sisibibi.api.global.security.session;

import com.sisibibi.api.domain.user.entity.UserStatus;
import com.sisibibi.api.domain.user.repository.UserRepository;
import com.sisibibi.api.domain.user.repository.UserRepository.UserSessionProjection;
import com.sisibibi.api.global.exception.CustomException;
import com.sisibibi.api.global.exception.ErrorCode;
import com.sisibibi.api.global.security.jwt.JwtTokenProvider.TokenClaims;
import com.sisibibi.api.global.security.jwt.JwtTokenProvider.TokenType;
import org.junit.jupiter.api.Test;

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
        given(userRepository.findSessionById(1L))
                .willReturn(Optional.of(session(UserStatus.ACTIVE, 2L)));

        assertThatCode(() -> tokenSessionValidator.validate(claims(2L)))
                .doesNotThrowAnyException();
    }

    @Test
    void validate_rejectsOldTokenVersion() {
        given(userRepository.findSessionById(1L))
                .willReturn(Optional.of(session(UserStatus.ACTIVE, 3L)));

        assertThatThrownBy(() -> tokenSessionValidator.validate(claims(2L)))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_TOKEN);
    }

    @Test
    void validate_rejectsBannedUser() {
        given(userRepository.findSessionById(1L))
                .willReturn(Optional.of(session(UserStatus.BANNED, 2L)));

        assertThatThrownBy(() -> tokenSessionValidator.validate(claims(2L)))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USER_BANNED);
    }

    private UserSessionProjection session(UserStatus status, Long tokenVersion) {
        return new UserSessionProjection() {
            @Override
            public UserStatus getStatus() {
                return status;
            }

            @Override
            public Long getTokenVersion() {
                return tokenVersion;
            }
        };
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
