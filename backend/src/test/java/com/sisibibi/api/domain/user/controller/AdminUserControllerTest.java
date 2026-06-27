package com.sisibibi.api.domain.user.controller;

import com.sisibibi.api.domain.user.dto.response.AdminUserRes;
import com.sisibibi.api.domain.user.entity.UserRole;
import com.sisibibi.api.domain.user.entity.UserStatus;
import com.sisibibi.api.domain.user.service.UserService;
import com.sisibibi.api.global.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminUserController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class AdminUserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @Test
    void getUsers_returnsFilteredUsers() throws Exception {
        given(userService.getUsersForAdmin(
                eq("tester"),
                eq(UserStatus.ACTIVE),
                eq(UserRole.USER),
                any(Pageable.class)
        )).willReturn(new PageImpl<>(List.of(response())));

        mockMvc.perform(get("/api/v1/admin/users")
                        .param("keyword", "tester")
                        .param("status", "ACTIVE")
                        .param("role", "USER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].userId").value(10))
                .andExpect(jsonPath("$.data.content[0].email").value("user@example.com"))
                .andExpect(jsonPath("$.data.content[0].nickname").value("tester"))
                .andExpect(jsonPath("$.data.content[0].password").doesNotExist());

        verify(userService).getUsersForAdmin(
                eq("tester"),
                eq(UserStatus.ACTIVE),
                eq(UserRole.USER),
                any(Pageable.class)
        );
    }

    @Test
    void getUsers_returnsUsersWithoutFilters() throws Exception {
        given(userService.getUsersForAdmin(
                isNull(),
                isNull(),
                isNull(),
                any(Pageable.class)
        )).willReturn(new PageImpl<>(List.of(response())));

        mockMvc.perform(get("/api/v1/admin/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].userId").value(10));
    }

    @Test
    void getUser_returnsUser() throws Exception {
        given(userService.getUserForAdmin(10L)).willReturn(response());

        mockMvc.perform(get("/api/v1/admin/users/{userId}", 10L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userId").value(10))
                .andExpect(jsonPath("$.data.email").value("user@example.com"))
                .andExpect(jsonPath("$.data.password").doesNotExist());
    }

    @Test
    void getUser_returnsBadRequest_whenUserIdIsInvalid() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users/{userId}", 0L))
                .andExpect(status().isBadRequest());
    }

    private AdminUserRes response() {
        LocalDateTime now = LocalDateTime.of(2026, 6, 27, 12, 0);
        return new AdminUserRes(
                10L,
                "user@example.com",
                "tester",
                UserRole.USER,
                UserStatus.ACTIVE,
                now,
                now
        );
    }
}
