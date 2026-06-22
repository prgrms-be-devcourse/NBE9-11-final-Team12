package com.sisibibi.api.global.security;

public record AuthPrincipal(
        Long userId,
        String email,
        String role,
        Long tokenVersion
) {

    public AuthPrincipal(Long userId, String email, String role) {
        this(userId, email, role, 0L);
    }
}
