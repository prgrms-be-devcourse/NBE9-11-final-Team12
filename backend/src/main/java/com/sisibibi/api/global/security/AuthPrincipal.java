package com.sisibibi.api.global.security;

public record AuthPrincipal(
        Long userId,
        String email,
        String role
) {
}
