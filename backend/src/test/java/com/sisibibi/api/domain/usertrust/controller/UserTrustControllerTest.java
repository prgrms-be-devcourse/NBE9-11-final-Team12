package com.sisibibi.api.domain.usertrust.controller;

import com.sisibibi.api.domain.usertrust.dto.response.UserTrustDetailRes;
import com.sisibibi.api.domain.usertrust.dto.response.UserTrustSummaryRes;
import com.sisibibi.api.domain.usertrust.entity.UserActivityLevel;
import com.sisibibi.api.domain.usertrust.entity.UserTrustLevel;
import com.sisibibi.api.domain.usertrust.service.UserTrustService;
import com.sisibibi.api.global.exception.GlobalExceptionHandler;
import com.sisibibi.api.global.security.AuthPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;
import java.time.LocalDateTime;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserTrustController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class UserTrustControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private UserTrustService userTrustService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getMyTrust_returnsDetailedTrust() throws Exception {
        given(userTrustService.getMyTrust(1L)).willReturn(new UserTrustDetailRes(
                1L, "tester", 72, UserTrustLevel.RELIABLE, UserActivityLevel.CONTRIBUTOR,
                22, 4, 3, 1, 15, 8, "v1",
                LocalDateTime.of(2026, 6, 23, 9, 0)
        ));

        mockMvc.perform(get("/api/v1/users/me/trust").with(authPrincipal()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.score").value(72))
                .andExpect(jsonPath("$.data.trustLevel").value("RELIABLE"))
                .andExpect(jsonPath("$.data.resolvedViolationCount").value(1));
    }

    @Test
    void getUserTrust_returnsPublicSummary() throws Exception {
        given(userTrustService.getUserTrust(2L)).willReturn(new UserTrustSummaryRes(
                2L, "other", 65, UserTrustLevel.NORMAL, UserActivityLevel.ACTIVE,
                "v1", LocalDateTime.of(2026, 6, 23, 9, 0)
        ));

        mockMvc.perform(get("/api/v1/users/2/trust").with(authPrincipal()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userId").value(2))
                .andExpect(jsonPath("$.data.score").value(65));
    }

    @Test
    void getUserTrust_returnsBadRequest_whenUserIdIsNotPositive() throws Exception {
        mockMvc.perform(get("/api/v1/users/0/trust").with(authPrincipal()))
                .andExpect(status().isBadRequest());
    }

    private RequestPostProcessor authPrincipal() {
        return request -> {
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(
                            new AuthPrincipal(1L, "user@example.com", "USER"),
                            null,
                            List.of()
                    )
            );
            return request;
        };
    }
}
