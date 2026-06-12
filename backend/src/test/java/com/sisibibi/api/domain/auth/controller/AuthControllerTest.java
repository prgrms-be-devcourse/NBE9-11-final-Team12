package com.sisibibi.api.domain.auth.controller;

import com.sisibibi.api.ApiApplication;
import com.sisibibi.api.domain.auth.dto.response.AuthTokenResult;
import com.sisibibi.api.domain.auth.dto.response.LoginRes;
import com.sisibibi.api.domain.auth.dto.response.TokenReissueRes;
import com.sisibibi.api.domain.auth.dto.response.SignupRes;
import com.sisibibi.api.domain.auth.service.AuthService;
import com.sisibibi.api.global.exception.GlobalExceptionHandler;
import com.sisibibi.api.global.security.cookie.AuthCookieProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.ResponseCookie;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItems;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private AuthCookieProvider authCookieProvider;

    @Test
    void signup_returnsCreated() throws Exception {
        given(authService.signup(any()))
                .willReturn(new SignupRes(1L, "user@example.com", "tester"));

        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "user@example.com",
                                  "password": "password123!",
                                  "nickname": "tester"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(201))
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.userId").value(1))
                .andExpect(jsonPath("$.data.email").value("user@example.com"))
                .andExpect(jsonPath("$.data.nickname").value("tester"));

        verify(authService).signup(any());
    }

    @Test
    void login_returnsOkWithAuthCookies() throws Exception {
        given(authService.login(any()))
                .willReturn(new AuthTokenResult<>(
                        new LoginRes(1L, "user@example.com", "tester"),
                        "access-token",
                        "refresh-token",
                        "refresh-token-id"
                ));
        given(authCookieProvider.createAccessTokenCookie("access-token"))
                .willReturn(ResponseCookie.from("accessToken", "access-token").path("/api").build());
        given(authCookieProvider.createRefreshTokenCookie("refresh-token"))
                .willReturn(ResponseCookie.from("refreshToken", "refresh-token").path("/api/v1/auth").build());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "user@example.com",
                                  "password": "password123!"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(header().stringValues("Set-Cookie", hasItems(
                        containsString("accessToken=access-token"),
                        containsString("refreshToken=refresh-token"))))
                .andExpect(jsonPath("$.data.userId").value(1))
                .andExpect(jsonPath("$.data.email").value("user@example.com"))
                .andExpect(jsonPath("$.data.nickname").value("tester"));

        verify(authService).login(any());
    }

    @Test
    void reissue_returnsOkWithRotatedAuthCookies() throws Exception {
        given(authService.reissue("old-refresh-token"))
                .willReturn(new AuthTokenResult<>(
                        new TokenReissueRes(1L, "user@example.com", "tester"),
                        "new-access-token",
                        "new-refresh-token",
                        "new-refresh-token-id"
                ));
        given(authCookieProvider.createAccessTokenCookie("new-access-token"))
                .willReturn(ResponseCookie.from("accessToken", "new-access-token").path("/api").build());
        given(authCookieProvider.createRefreshTokenCookie("new-refresh-token"))
                .willReturn(ResponseCookie.from("refreshToken", "new-refresh-token").path("/api/v1/auth").build());

        mockMvc.perform(post("/api/v1/auth/reissue")
                        .cookie(new jakarta.servlet.http.Cookie("refreshToken", "old-refresh-token")))
                .andExpect(status().isOk())
                .andExpect(header().stringValues("Set-Cookie", hasItems(
                        containsString("accessToken=new-access-token"),
                        containsString("refreshToken=new-refresh-token"))))
                .andExpect(jsonPath("$.data.userId").value(1));

        verify(authService).reissue("old-refresh-token");
    }

    @Test
    void logout_returnsOkAndExpiresAuthCookies() throws Exception {
        given(authCookieProvider.expireAccessTokenCookie())
                .willReturn(ResponseCookie.from("accessToken", "").path("/api").maxAge(0).build());
        given(authCookieProvider.expireRefreshTokenCookie())
                .willReturn(ResponseCookie.from("refreshToken", "").path("/api/v1/auth").maxAge(0).build());

        mockMvc.perform(post("/api/v1/auth/logout")
                        .cookie(new jakarta.servlet.http.Cookie("refreshToken", "refresh-token")))
                .andExpect(status().isOk())
                .andExpect(header().stringValues("Set-Cookie", hasItems(
                        containsString("accessToken="),
                        containsString("refreshToken="))))
                .andExpect(jsonPath("$.code").value("SUCCESS"));

        verify(authService).logout("refresh-token");
    }

    @Test
    void signup_returnsBadRequest_whenEmailIsInvalid() throws Exception {
        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "invalid-email",
                                  "password": "password123!",
                                  "nickname": "tester"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"))
                .andExpect(jsonPath("$.data.email").exists());
    }
}
