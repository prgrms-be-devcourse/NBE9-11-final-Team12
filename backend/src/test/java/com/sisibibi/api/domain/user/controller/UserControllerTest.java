package com.sisibibi.api.domain.user.controller;

import com.sisibibi.api.domain.user.dto.response.UserMeRes;
import com.sisibibi.api.domain.user.entity.UserRole;
import com.sisibibi.api.domain.user.entity.UserStatus;
import com.sisibibi.api.domain.user.service.UserService;
import com.sisibibi.api.global.exception.GlobalExceptionHandler;
import com.sisibibi.api.global.security.AuthPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getMe_returnsCurrentUser() throws Exception {
        given(userService.getMe(1L))
                .willReturn(new UserMeRes(1L, "user@example.com", "tester", UserRole.USER, UserStatus.ACTIVE));

        mockMvc.perform(get("/api/v1/users/me")
                        .with(authPrincipal()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userId").value(1))
                .andExpect(jsonPath("$.data.email").value("user@example.com"))
                .andExpect(jsonPath("$.data.nickname").value("tester"));

        verify(userService).getMe(1L);
    }

    @Test
    void updateMe_returnsUpdatedUser() throws Exception {
        given(userService.updateMe(org.mockito.ArgumentMatchers.eq(1L), org.mockito.ArgumentMatchers.any()))
                .willReturn(new UserMeRes(1L, "user@example.com", "newbie", UserRole.USER, UserStatus.ACTIVE));

        mockMvc.perform(patch("/api/v1/users/me")
                        .with(authPrincipal())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nickname": "newbie"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nickname").value("newbie"));

        verify(userService).updateMe(org.mockito.ArgumentMatchers.eq(1L), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void updateMe_returnsBadRequest_whenNicknameIsBlank() throws Exception {
        mockMvc.perform(patch("/api/v1/users/me")
                        .with(authPrincipal())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nickname": ""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"))
                .andExpect(jsonPath("$.data.nickname").exists());
    }

    private UsernamePasswordAuthenticationToken authToken() {
        return new UsernamePasswordAuthenticationToken(
                new AuthPrincipal(1L, "user@example.com", "USER"),
                null,
                List.of()
        );
    }

    private RequestPostProcessor authPrincipal() {
        return request -> {
            SecurityContextHolder.getContext().setAuthentication(authToken());
            return request;
        };
    }
}
