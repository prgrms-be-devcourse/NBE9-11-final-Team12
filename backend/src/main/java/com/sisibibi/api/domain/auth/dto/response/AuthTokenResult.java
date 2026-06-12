package com.sisibibi.api.domain.auth.dto.response;

public record AuthTokenResult<T>(
        T response,
        String accessToken,
        String refreshToken
) {
}
