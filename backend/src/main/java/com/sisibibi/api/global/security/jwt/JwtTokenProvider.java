package com.sisibibi.api.global.security.jwt;

import com.sisibibi.api.global.exception.CustomException;
import com.sisibibi.api.global.exception.ErrorCode;
import com.sisibibi.api.global.security.AuthPrincipal;
import com.sisibibi.api.global.security.config.AuthProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtTokenProvider {

    private static final String EMAIL_CLAIM = "email";
    private static final String ROLE_CLAIM = "role";
    private static final String TOKEN_TYPE_CLAIM = "tokenType";
    private static final String TOKEN_VERSION_CLAIM = "tokenVersion";

    private final AuthProperties authProperties;
    private final Clock clock;
    private final SecretKey secretKey;

    @Autowired
    public JwtTokenProvider(AuthProperties authProperties) {
        this(authProperties, Clock.systemUTC());
    }

    JwtTokenProvider(AuthProperties authProperties, Clock clock) {
        this.authProperties = authProperties;
        this.clock = clock;
        this.secretKey = Keys.hmacShaKeyFor(
                authProperties.jwt().secret().getBytes(StandardCharsets.UTF_8)
        );
    }

    public String createAccessToken(AuthPrincipal principal) {
        return createToken(principal, TokenType.ACCESS, UUID.randomUUID().toString());
    }

    public String createRefreshToken(AuthPrincipal principal, String tokenId) {
        return createToken(principal, TokenType.REFRESH, tokenId);
    }

    public TokenClaims parseAccessToken(String token) {
        TokenClaims claims = parseToken(token);
        validateTokenType(claims, TokenType.ACCESS);
        return claims;
    }

    public TokenClaims parseRefreshToken(String token) {
        TokenClaims claims = parseToken(token);
        validateTokenType(claims, TokenType.REFRESH);
        return claims;
    }

    private String createToken(AuthPrincipal principal, TokenType tokenType, String tokenId) {
        Instant now = Instant.now(clock);
        Instant expiresAt = now.plus(expirationBy(tokenType));

        return Jwts.builder()
                .subject(String.valueOf(principal.userId()))
                .id(tokenId)
                .claim(EMAIL_CLAIM, principal.email())
                .claim(ROLE_CLAIM, principal.role())
                .claim(TOKEN_TYPE_CLAIM, tokenType.name())
                .claim(TOKEN_VERSION_CLAIM, principal.tokenVersion())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(secretKey)
                .compact();
    }

    private TokenClaims parseToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            return new TokenClaims(
                    Long.valueOf(claims.getSubject()),
                    claims.get(EMAIL_CLAIM, String.class),
                    claims.get(ROLE_CLAIM, String.class),
                    claims.getId(),
                    TokenType.valueOf(claims.get(TOKEN_TYPE_CLAIM, String.class)),
                    readTokenVersion(claims),
                    claims.getExpiration().toInstant()
            );
        } catch (ExpiredJwtException e) {
            throw new CustomException(ErrorCode.EXPIRED_TOKEN);
        } catch (RuntimeException e) {
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }
    }

    private void validateTokenType(TokenClaims claims, TokenType expectedType) {
        if (claims.tokenType() != expectedType) {
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }
    }

    private java.time.Duration expirationBy(TokenType tokenType) {
        return switch (tokenType) {
            case ACCESS -> authProperties.jwt().accessTokenExpiration();
            case REFRESH -> authProperties.jwt().refreshTokenExpiration();
        };
    }

    public record TokenClaims(
            Long userId,
            String email,
            String role,
            String tokenId,
            TokenType tokenType,
            Long tokenVersion,
            Instant expiresAt
    ) {

        public TokenClaims(
                Long userId,
                String email,
                String role,
                String tokenId,
                TokenType tokenType,
                Instant expiresAt
        ) {
            this(userId, email, role, tokenId, tokenType, 0L, expiresAt);
        }

        public AuthPrincipal toPrincipal() {
            return new AuthPrincipal(userId, email, role, tokenVersion);
        }
    }

    private Long readTokenVersion(Claims claims) {
        Object tokenVersion = claims.get(TOKEN_VERSION_CLAIM);
        if (!(tokenVersion instanceof Number number)) {
            throw new IllegalArgumentException("Token version claim is missing.");
        }
        return number.longValue();
    }

    public enum TokenType {
        ACCESS,
        REFRESH
    }
}
