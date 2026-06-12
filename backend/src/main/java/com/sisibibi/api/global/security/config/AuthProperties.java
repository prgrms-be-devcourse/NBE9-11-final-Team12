package com.sisibibi.api.global.security.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "app.auth")
public record AuthProperties(
        @NotNull
        Jwt jwt,

        @NotNull
        Cookie cookie
) {

    public record Jwt(
            @NotBlank
            @Size(min = 32)
            String secret,

            @NotNull
            Duration accessTokenExpiration,

            @NotNull
            Duration refreshTokenExpiration
    ) {
    }

    public record Cookie(
            boolean secure,

            @NotBlank
            String sameSite,

            String domain
    ) {
    }
}