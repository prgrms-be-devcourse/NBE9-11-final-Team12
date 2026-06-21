package com.sisibibi.api.domain.usersanction.controller;

import com.sisibibi.api.ApiApplication;
import com.sisibibi.api.domain.usersanction.dto.response.UserSanctionRes;
import com.sisibibi.api.domain.usersanction.entity.UserSanctionState;
import com.sisibibi.api.domain.usersanction.entity.UserSanctionType;
import com.sisibibi.api.domain.usersanction.service.UserSanctionService;
import com.sisibibi.api.global.exception.GlobalExceptionHandler;
import com.sisibibi.api.global.security.AuthPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminUserSanctionController.class)
@AutoConfigureMockMvc(addFilters = false)
@ContextConfiguration(classes = {
        ApiApplication.class,
        AdminUserSanctionController.class,
        GlobalExceptionHandler.class
})
class AdminUserSanctionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserSanctionService userSanctionService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createSanction_returnsCreated() throws Exception {
        given(userSanctionService.createSanction(eq(10L), eq(99L), any()))
                .willReturn(response(UserSanctionState.ACTIVE));

        mockMvc.perform(post("/api/v1/admin/users/{userId}/sanctions", 10L)
                        .with(authPrincipal(99L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "type": "CHAT_RESTRICTION",
                                  "reason": "반복적인 채팅 도배",
                                  "durationHours": 24,
                                  "reportId": 100
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.sanctionId").value(200))
                .andExpect(jsonPath("$.data.state").value("ACTIVE"));
    }

    @Test
    void createSanction_returnsBadRequest_whenReasonIsBlank() throws Exception {
        mockMvc.perform(post("/api/v1/admin/users/{userId}/sanctions", 10L)
                        .with(authPrincipal(99L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "type": "CHAT_RESTRICTION",
                                  "reason": " ",
                                  "durationHours": 24
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.reason").value("제재 사유는 필수입니다."));
    }

    @Test
    void revokeSanction_returnsOk() throws Exception {
        given(userSanctionService.revokeSanction(10L, 200L, 99L, "오인 제재 확인"))
                .willReturn(response(UserSanctionState.REVOKED));

        mockMvc.perform(patch(
                        "/api/v1/admin/users/{userId}/sanctions/{sanctionId}/revoke",
                        10L,
                        200L
                )
                        .with(authPrincipal(99L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reason": "오인 제재 확인"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.state").value("REVOKED"));
    }

    private UserSanctionRes response(UserSanctionState state) {
        LocalDateTime now = LocalDateTime.of(2026, 6, 21, 12, 0);
        return new UserSanctionRes(
                200L,
                10L,
                99L,
                100L,
                UserSanctionType.CHAT_RESTRICTION,
                "반복적인 채팅 도배",
                state,
                now,
                now.plusHours(24),
                state == UserSanctionState.REVOKED ? now.plusHours(1) : null,
                state == UserSanctionState.REVOKED ? 99L : null,
                state == UserSanctionState.REVOKED ? "오인 제재 확인" : null,
                now
        );
    }

    private RequestPostProcessor authPrincipal(Long userId) {
        return request -> {
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(
                            new AuthPrincipal(userId, "admin@example.com", "ADMIN"),
                            null,
                            List.of()
                    )
            );
            return request;
        };
    }
}
