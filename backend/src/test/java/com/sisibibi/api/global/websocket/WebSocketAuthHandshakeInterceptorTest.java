package com.sisibibi.api.global.websocket;

import com.sisibibi.api.global.security.AuthPrincipal;
import com.sisibibi.api.global.security.config.AuthProperties;
import com.sisibibi.api.global.security.cookie.AuthCookieProvider;
import com.sisibibi.api.global.security.jwt.JwtTokenProvider;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.http.server.ServletServerHttpRequest;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class WebSocketAuthHandshakeInterceptorTest {

    private JwtTokenProvider jwtTokenProvider;
    private WebSocketAuthHandshakeInterceptor interceptor;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider(authProperties());
        interceptor = new WebSocketAuthHandshakeInterceptor(jwtTokenProvider);
    }

    @Test
    void beforeHandshake_storesPrincipal_whenAccessTokenCookieIsValid() {
        AuthPrincipal principal = new AuthPrincipal(1L, "user@example.com", "USER");
        String accessToken = jwtTokenProvider.createAccessToken(principal);
        MockHttpServletRequest servletRequest = new MockHttpServletRequest();
        servletRequest.setCookies(new Cookie(AuthCookieProvider.ACCESS_TOKEN_COOKIE_NAME, accessToken));
        Map<String, Object> attributes = new HashMap<>();

        boolean result = interceptor.beforeHandshake(
                new ServletServerHttpRequest(servletRequest),
                mock(ServerHttpResponse.class),
                null,
                attributes
        );

        assertThat(result).isTrue();
        assertThat(attributes.get(WebSocketAuthAttributes.AUTH_PRINCIPAL)).isEqualTo(principal);
    }

    @Test
    void beforeHandshake_rejects_whenAccessTokenCookieIsMissing() {
        MockHttpServletRequest servletRequest = new MockHttpServletRequest();
        Map<String, Object> attributes = new HashMap<>();

        boolean result = interceptor.beforeHandshake(
                new ServletServerHttpRequest(servletRequest),
                mock(ServerHttpResponse.class),
                null,
                attributes
        );

        assertThat(result).isFalse();
        assertThat(attributes).doesNotContainKey(WebSocketAuthAttributes.AUTH_PRINCIPAL);
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
