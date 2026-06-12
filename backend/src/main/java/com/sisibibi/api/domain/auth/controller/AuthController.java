package com.sisibibi.api.domain.auth.controller;

import com.sisibibi.api.domain.auth.dto.request.LoginReq;
import com.sisibibi.api.domain.auth.dto.request.SignupReq;
import com.sisibibi.api.domain.auth.dto.response.AuthTokenResult;
import com.sisibibi.api.domain.auth.dto.response.LoginRes;
import com.sisibibi.api.domain.auth.dto.response.SignupRes;
import com.sisibibi.api.domain.auth.dto.response.TokenReissueRes;
import com.sisibibi.api.domain.auth.service.AuthService;
import com.sisibibi.api.global.response.ApiResponse;
import com.sisibibi.api.global.security.cookie.AuthCookieProvider;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final AuthCookieProvider authCookieProvider;

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<SignupRes>> signup(
            @Valid @RequestBody SignupReq request
    ) {
        SignupRes response = authService.signup(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.created("회원가입이 완료되었습니다.", response));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginRes>> login(
            @Valid @RequestBody LoginReq request
    ) {
        AuthTokenResult<LoginRes> result = authService.login(request);

        return withAuthCookies(result)
                .body(ApiResponse.ok("로그인이 완료되었습니다.", result.response()));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @CookieValue(
                    name = AuthCookieProvider.REFRESH_TOKEN_COOKIE_NAME,
                    required = false
            ) String refreshToken
    ) {
        authService.logout(refreshToken);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, authCookieProvider.expireAccessTokenCookie().toString())
                .header(HttpHeaders.SET_COOKIE, authCookieProvider.expireRefreshTokenCookie().toString())
                .body(ApiResponse.okMessage("로그아웃이 완료되었습니다."));
    }

    @PostMapping("/reissue")
    public ResponseEntity<ApiResponse<TokenReissueRes>> reissue(
            @CookieValue(
                    name = AuthCookieProvider.REFRESH_TOKEN_COOKIE_NAME,
                    required = false
            ) String refreshToken
    ) {
        AuthTokenResult<TokenReissueRes> result = authService.reissue(refreshToken);

        return withAuthCookies(result)
                .body(ApiResponse.ok("토큰 재발급이 완료되었습니다.", result.response()));
    }

    private <T> ResponseEntity.BodyBuilder withAuthCookies(AuthTokenResult<T> result) {
        ResponseCookie accessTokenCookie = authCookieProvider.createAccessTokenCookie(result.accessToken());
        ResponseCookie refreshTokenCookie = authCookieProvider.createRefreshTokenCookie(result.refreshToken());

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, accessTokenCookie.toString())
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString());
    }
}
