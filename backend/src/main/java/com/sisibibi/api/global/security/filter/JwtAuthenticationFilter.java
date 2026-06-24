package com.sisibibi.api.global.security.filter;

import com.sisibibi.api.global.exception.CustomException;
import com.sisibibi.api.global.security.AuthPrincipal;
import com.sisibibi.api.global.security.cookie.AuthCookieProvider;
import com.sisibibi.api.global.security.handler.SecurityExceptionHandler;
import com.sisibibi.api.global.security.jwt.JwtTokenProvider;
import com.sisibibi.api.global.security.jwt.JwtTokenProvider.TokenClaims;
import com.sisibibi.api.global.security.session.TokenSessionValidator;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@ConditionalOnBean(JwtTokenProvider.class)
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTH_API_PREFIX = "/api/v1/auth/";

    private final JwtTokenProvider jwtTokenProvider;
    private final SecurityExceptionHandler securityExceptionHandler;
    private final TokenSessionValidator tokenSessionValidator;

    public JwtAuthenticationFilter(
            JwtTokenProvider jwtTokenProvider,
            SecurityExceptionHandler securityExceptionHandler,
            TokenSessionValidator tokenSessionValidator
    ) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.securityExceptionHandler = securityExceptionHandler;
        this.tokenSessionValidator = tokenSessionValidator;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String accessToken = findCookieValue(request, AuthCookieProvider.ACCESS_TOKEN_COOKIE_NAME);

        if (accessToken == null || accessToken.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            TokenClaims claims = jwtTokenProvider.parseAccessToken(accessToken);
            tokenSessionValidator.validate(claims);
            AuthPrincipal principal = claims.toPrincipal();

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            principal,
                            null,
                            List.of(new SimpleGrantedAuthority("ROLE_" + principal.role()))
                    );

            SecurityContextHolder.getContext().setAuthentication(authentication);
            filterChain.doFilter(request, response);
        } catch (CustomException e) {
            SecurityContextHolder.clearContext();
            securityExceptionHandler.write(response, e.getErrorCode());
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return request.getRequestURI().startsWith(AUTH_API_PREFIX);
    }

    private String findCookieValue(HttpServletRequest request, String cookieName) {
        Cookie[] cookies = request.getCookies();
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
