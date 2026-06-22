package com.sisibibi.api.global.security.filter;

import com.sisibibi.api.global.exception.ErrorCode;
import com.sisibibi.api.global.security.cookie.AuthCookieProvider;
import com.sisibibi.api.global.security.handler.SecurityExceptionHandler;
import com.sisibibi.api.global.security.jwt.JwtTokenProvider;
import com.sisibibi.api.global.security.session.TokenSessionValidator;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.Instant;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class JwtAuthenticationFilterTest {

    private final JwtTokenProvider jwtTokenProvider = mock(JwtTokenProvider.class);
    private final SecurityExceptionHandler securityExceptionHandler =
            mock(SecurityExceptionHandler.class);
    private final TokenSessionValidator tokenSessionValidator =
            mock(TokenSessionValidator.class);
    private final JwtAuthenticationFilter filter = new JwtAuthenticationFilter(
            jwtTokenProvider,
            securityExceptionHandler,
            tokenSessionValidator
    );

    @Test
    void doFilter_rejectsAccessToken_whenUserIsBanned() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/users/me");
        request.setCookies(new Cookie(AuthCookieProvider.ACCESS_TOKEN_COOKIE_NAME, "access-token"));
        MockHttpServletResponse response = new MockHttpServletResponse();
        var filterChain = mock(jakarta.servlet.FilterChain.class);
        var claims = new JwtTokenProvider.TokenClaims(
                10L,
                "user@example.com",
                "USER",
                null,
                JwtTokenProvider.TokenType.ACCESS,
                Instant.now().plusSeconds(300)
        );
        given(jwtTokenProvider.parseAccessToken("access-token")).willReturn(claims);
        org.mockito.BDDMockito.willThrow(
                new com.sisibibi.api.global.exception.CustomException(ErrorCode.USER_BANNED)
        ).given(tokenSessionValidator).validate(claims);

        filter.doFilter(request, response, filterChain);

        verify(securityExceptionHandler).write(response, ErrorCode.USER_BANNED);
        verifyNoInteractions(filterChain);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/api/v1/auth/signup",
            "/api/v1/auth/login",
            "/api/v1/auth/logout",
            "/api/v1/auth/reissue"
    })
    void doFilter_skipsAccessTokenValidation_forAuthApi(String requestUri) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", requestUri);
        request.setRequestURI(requestUri);
        request.setCookies(new Cookie(AuthCookieProvider.ACCESS_TOKEN_COOKIE_NAME, "stale-token"));
        MockHttpServletResponse response = new MockHttpServletResponse();
        var filterChain = mock(jakarta.servlet.FilterChain.class);

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtTokenProvider, tokenSessionValidator, securityExceptionHandler);
    }
}
