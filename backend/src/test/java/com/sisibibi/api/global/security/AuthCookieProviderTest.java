package com.sisibibi.api.global.security;

import com.sisibibi.api.global.security.config.AuthProperties;
import com.sisibibi.api.global.security.cookie.AuthCookieProvider;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseCookie;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class AuthCookieProviderTest {

    @Test
    void createAccessTokenCookie_appliesSecurityAttributes() {
        AuthCookieProvider cookieProvider = new AuthCookieProvider(authProperties());

        ResponseCookie cookie = cookieProvider.createAccessTokenCookie("access-token");

        String headerValue = cookie.toString();
        assertThat(headerValue).contains("accessToken=access-token");
        assertThat(headerValue).contains("Path=/api");
        assertThat(headerValue).contains("Max-Age=1800");
        assertThat(headerValue).contains("HttpOnly");
        assertThat(headerValue).contains("SameSite=Lax");
        assertThat(headerValue).doesNotContain("Secure");
    }

    @Test
    void createRefreshTokenCookie_usesReissuePath() {
        AuthCookieProvider cookieProvider = new AuthCookieProvider(authProperties());

        ResponseCookie cookie = cookieProvider.createRefreshTokenCookie("refresh-token");

        assertThat(cookie.toString()).contains("Path=/api/v1/auth/reissue");
        assertThat(cookie.toString()).contains("Max-Age=1209600");
    }

    @Test
    void expireCookies_useZeroMaxAge() {
        AuthCookieProvider cookieProvider = new AuthCookieProvider(authProperties());

        assertThat(cookieProvider.expireAccessTokenCookie().toString()).contains("Max-Age=0");
        assertThat(cookieProvider.expireRefreshTokenCookie().toString()).contains("Max-Age=0");
    }

    private AuthProperties authProperties() {
        return new AuthProperties(
                new AuthProperties.Jwt(
                        "test-jwt-secret-key-must-be-at-least-32-bytes-long",
                        Duration.ofMinutes(30),
                        Duration.ofDays(14)
                ),
                new AuthProperties.Cookie(false, "Lax", null)
        );
    }
}
