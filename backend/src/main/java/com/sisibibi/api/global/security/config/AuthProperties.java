package com.sisibibi.api.global.security.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.auth")
public record AuthProperties(
        Jwt jwt,
        Cookie cookie
) {

    public record Jwt(
            String secret,
            Duration accessTokenExpiration,
            Duration refreshTokenExpiration
    ) {
    }

    public record Cookie(
            boolean secure,
            String sameSite,
            String domain
    ) {
    }
}
