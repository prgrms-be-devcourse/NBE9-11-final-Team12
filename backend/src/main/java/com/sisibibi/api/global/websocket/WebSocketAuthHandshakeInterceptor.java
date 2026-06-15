package com.sisibibi.api.global.websocket;

import com.sisibibi.api.global.exception.CustomException;
import com.sisibibi.api.global.security.cookie.AuthCookieProvider;
import com.sisibibi.api.global.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketAuthHandshakeInterceptor implements HandshakeInterceptor {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public boolean beforeHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes
    ) {
        String accessToken = findCookieValue(
                request.getHeaders().get(HttpHeaders.COOKIE),
                AuthCookieProvider.ACCESS_TOKEN_COOKIE_NAME
        );

        if (accessToken == null || accessToken.isBlank()) {
            return false;
        }

        try {
            attributes.put(
                    WebSocketAuthAttributes.AUTH_PRINCIPAL,
                    jwtTokenProvider.parseAccessToken(accessToken).toPrincipal()
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

    private String findCookieValue(List<String> cookieHeaders, String cookieName) {
        if (cookieHeaders == null) {
            return null;
        }

        for (String cookieHeader : cookieHeaders) {
            String[] cookies = cookieHeader.split(";");
            for (String cookie : cookies) {
                String[] parts = cookie.trim().split("=", 2);
                if (parts.length == 2 && cookieName.equals(parts[0])) {
                    return parts[1];
                }
            }
        }

        return null;
    }
}
