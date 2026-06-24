package com.sisibibi.api.global.websocket;

import com.sisibibi.api.global.exception.CustomException;
import com.sisibibi.api.global.security.cookie.AuthCookieProvider;
import com.sisibibi.api.global.security.jwt.JwtTokenProvider;
import com.sisibibi.api.global.security.jwt.JwtTokenProvider.TokenClaims;
import com.sisibibi.api.global.security.session.TokenSessionValidator;
import jakarta.servlet.http.Cookie;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketAuthHandshakeInterceptor implements HandshakeInterceptor {

    private final JwtTokenProvider jwtTokenProvider;
    private final TokenSessionValidator tokenSessionValidator;

    @Override
    public boolean beforeHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes
    ) {
        String accessToken = findCookieValue(
                request,
                AuthCookieProvider.ACCESS_TOKEN_COOKIE_NAME
        );

        if (accessToken == null || accessToken.isBlank()) {
            return false;
        }

        try {
            TokenClaims claims = jwtTokenProvider.parseAccessToken(accessToken);
            tokenSessionValidator.validate(claims);
            attributes.put(
                    WebSocketAuthAttributes.AUTH_PRINCIPAL,
                    claims.toPrincipal()
            );
            return true;
        } catch (CustomException e) {
            log.warn("WebSocket handshake authentication failed. code={}", e.getErrorCode().name());
            return false;
        }
    }

    @Override
    public void afterHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Exception exception
    ) {
    }

    private String findCookieValue(ServerHttpRequest request, String cookieName) {
        if (!(request instanceof ServletServerHttpRequest servletRequest)) {
            return null;
        }

        Cookie[] cookies = servletRequest.getServletRequest().getCookies();
        if (cookies == null) {
            return null;
        }

        for (Cookie cookie : cookies) {
            if (cookieName.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }

        return null;
    }
}
