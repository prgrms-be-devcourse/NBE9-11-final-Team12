package com.sisibibi.api.global.security.jwt;

import com.sisibibi.api.global.exception.CustomException;
import com.sisibibi.api.global.exception.ErrorCode;
import com.sisibibi.api.global.security.AuthPrincipal;
import com.sisibibi.api.global.security.config.AuthProperties;
import com.sisibibi.api.global.security.jwt.JwtTokenProvider.TokenClaims;
import com.sisibibi.api.global.security.jwt.JwtTokenProvider.TokenType;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtTokenProviderTest {

    private static final String SECRET =
            "test-jwt-secret-key-must-be-at-least-32-bytes-long";

    private final Clock clock = Clock.fixed(
            Instant.parse("2030-06-12T00:00:00Z"),
            ZoneOffset.UTC
    );

    @Test
    void createAccessToken_containsPrincipalAndExpiration() {
        JwtTokenProvider tokenProvider = new JwtTokenProvider(authProperties(
                Duration.ofMinutes(30),
                Duration.ofDays(14)
        ), clock);
        AuthPrincipal principal = new AuthPrincipal(1L, "user@example.com", "USER", 3L);

        String token = tokenProvider.createAccessToken(principal);

        TokenClaims claims = tokenProvider.parseAccessToken(token);
        assertThat(claims.userId()).isEqualTo(1L);
        assertThat(claims.email()).isEqualTo("user@example.com");
        assertThat(claims.role()).isEqualTo("USER");
        assertThat(claims.tokenVersion()).isEqualTo(3L);
        assertThat(claims.tokenType()).isEqualTo(TokenType.ACCESS);
        assertThat(claims.expiresAt()).isEqualTo(Instant.parse("2030-06-12T00:30:00Z"));
    }

    @Test
    void parseAccessToken_rejectsInvalidToken() {
        JwtTokenProvider tokenProvider = new JwtTokenProvider(authProperties(
                Duration.ofMinutes(30),
                Duration.ofDays(14)
        ), clock);

        assertThatThrownBy(() -> tokenProvider.parseAccessToken("invalid-token"))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_TOKEN);
    }

    @Test
    void parseAccessToken_rejectsExpiredToken() {
        Clock pastClock = Clock.fixed(Instant.parse("2020-06-12T00:00:00Z"), ZoneOffset.UTC);
        JwtTokenProvider tokenProvider = new JwtTokenProvider(authProperties(
                Duration.ofMinutes(30),
                Duration.ofDays(14)
        ), pastClock);
        AuthPrincipal principal = new AuthPrincipal(1L, "user@example.com", "USER");
        String token = tokenProvider.createAccessToken(principal);

        assertThatThrownBy(() -> tokenProvider.parseAccessToken(token))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.EXPIRED_TOKEN);
    }

    @Test
    void parseAccessToken_rejectsRefreshToken() {
        JwtTokenProvider tokenProvider = new JwtTokenProvider(authProperties(
                Duration.ofMinutes(30),
                Duration.ofDays(14)
        ), clock);
        AuthPrincipal principal = new AuthPrincipal(1L, "user@example.com", "USER");
        String refreshToken = tokenProvider.createRefreshToken(principal, "refresh-token-id");

        assertThatThrownBy(() -> tokenProvider.parseAccessToken(refreshToken))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_TOKEN);
    }

    private AuthProperties authProperties(
            Duration accessTokenExpiration,
            Duration refreshTokenExpiration
    ) {
        return new AuthProperties(
                new AuthProperties.Jwt(SECRET, accessTokenExpiration, refreshTokenExpiration),
                new AuthProperties.Cookie(false, "Lax", null)
        );
    }
}
