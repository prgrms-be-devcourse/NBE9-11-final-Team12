package com.sisibibi.api.global.security.cookie;

import com.sisibibi.api.global.security.config.AuthProperties;

import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class AuthCookieProvider {

    public static final String ACCESS_TOKEN_COOKIE_NAME = "accessToken";
    public static final String REFRESH_TOKEN_COOKIE_NAME = "refreshToken";
    public static final String ACCESS_TOKEN_PATH = "/api";
    public static final String REFRESH_TOKEN_PATH = "/api/v1/auth";

    private final AuthProperties authProperties;

    public AuthCookieProvider(AuthProperties authProperties) {
        this.authProperties = authProperties;
    }

    public ResponseCookie createAccessTokenCookie(String token) {
        return createCookie(
                ACCESS_TOKEN_COOKIE_NAME,
                token,
                ACCESS_TOKEN_PATH,
                authProperties.jwt().accessTokenExpiration()
        );
    }

    public ResponseCookie createRefreshTokenCookie(String token) {
        return createCookie(
                REFRESH_TOKEN_COOKIE_NAME,
                token,
                REFRESH_TOKEN_PATH,
                authProperties.jwt().refreshTokenExpiration()
        );
    }

    public ResponseCookie expireAccessTokenCookie() {
        return expireCookie(ACCESS_TOKEN_COOKIE_NAME, ACCESS_TOKEN_PATH);
    }

    public ResponseCookie expireRefreshTokenCookie() {
        return expireCookie(REFRESH_TOKEN_COOKIE_NAME, REFRESH_TOKEN_PATH);
    }

    private ResponseCookie createCookie(
            String name,
            String value,
            String path,
            Duration maxAge
    ) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(authProperties.cookie().secure())
                .sameSite(authProperties.cookie().sameSite())
                .path(path)
                .maxAge(maxAge);

        applyDomain(builder);
        return builder.build();
    }

    private ResponseCookie expireCookie(String name, String path) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(name, "")
                .httpOnly(true)
                .secure(authProperties.cookie().secure())
                .sameSite(authProperties.cookie().sameSite())
                .path(path)
                .maxAge(Duration.ZERO);

        applyDomain(builder);
        return builder.build();
    }

    private void applyDomain(ResponseCookie.ResponseCookieBuilder builder) {
        String domain = authProperties.cookie().domain();
        if (domain != null && !domain.isBlank()) {
            builder.domain(domain);
        }
    }
}
