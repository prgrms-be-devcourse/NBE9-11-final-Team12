package com.sisibibi.api.domain.user.controller;

import com.sisibibi.api.domain.user.dto.response.AdminUserRes;
import com.sisibibi.api.domain.user.entity.UserRole;
import com.sisibibi.api.domain.user.entity.UserStatus;
import com.sisibibi.api.domain.user.service.UserService;
import com.sisibibi.api.global.response.ApiResponse;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/users")
public class AdminUserController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<AdminUserRes>>> getUsers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) UserStatus status,
            @RequestParam(required = false) UserRole role,
            @PageableDefault(
                    size = 20,
                    sort = {"createdAt", "id"},
                    direction = Sort.Direction.DESC
            ) Pageable pageable
    ) {
        Page<AdminUserRes> response =
                userService.getUsersForAdmin(keyword, status, role, pageable);

        return ResponseEntity.ok(ApiResponse.ok("관리자 사용자 목록 조회가 완료되었습니다.", response));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<AdminUserRes>> getUser(
            @PathVariable @Positive Long userId
    ) {
        AdminUserRes response = userService.getUserForAdmin(userId);

        return ResponseEntity.ok(ApiResponse.ok("관리자 사용자 상세 조회가 완료되었습니다.", response));
    }
}
