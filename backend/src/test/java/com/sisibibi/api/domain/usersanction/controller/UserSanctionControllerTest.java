package com.sisibibi.api.domain.usersanction.controller;

import com.sisibibi.api.ApiApplication;
import com.sisibibi.api.domain.usersanction.dto.response.ActiveUserSanctionRes;
import com.sisibibi.api.domain.usersanction.entity.UserSanctionType;
import com.sisibibi.api.domain.usersanction.service.UserSanctionService;
import com.sisibibi.api.global.exception.GlobalExceptionHandler;
import com.sisibibi.api.global.security.AuthPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserSanctionController.class)
@AutoConfigureMockMvc(addFilters = false)
@ContextConfiguration(classes = {
        ApiApplication.class,
        UserSanctionController.class,
        GlobalExceptionHandler.class
})
class UserSanctionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserSanctionService userSanctionService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getActiveSanctions_returnsAuthenticatedUsersRestrictions() throws Exception {
        LocalDateTime startsAt = LocalDateTime.of(2026, 6, 22, 12, 0);
        LocalDateTime endsAt = startsAt.plusHours(24);
        given(userSanctionService.getActiveSanctions(10L))
                .willReturn(List.of(new ActiveUserSanctionRes(
                        200L,
                        UserSanctionType.CHAT_RESTRICTION,
                        "반복적인 채팅 도배",
                        startsAt,
                        endsAt
                )));

        mockMvc.perform(get("/api/v1/users/me/sanctions/active")
                        .with(authPrincipal(10L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].sanctionId").value(200))
                .andExpect(jsonPath("$.data[0].type").value("CHAT_RESTRICTION"))
                .andExpect(jsonPath("$.data[0].endsAt").value("2026-06-23T12:00:00"));
    }

    private RequestPostProcessor authPrincipal(Long userId) {
        return request -> {
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(
                            new AuthPrincipal(userId, "user@example.com", "USER"),
                            null,
                            List.of()
                    )
            );
            return request;
        };
    }
}
