package com.sisibibi.api.domain.user.controller;

import com.sisibibi.api.domain.user.dto.request.UpdateUserReq;
import com.sisibibi.api.domain.user.dto.response.UserMeRes;
import com.sisibibi.api.domain.user.service.UserService;
import com.sisibibi.api.global.response.ApiResponse;
import com.sisibibi.api.global.security.AuthPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "사용자", description = "내 정보 조회 및 수정 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    @Operation(
        summary = "내 정보 조회",
        description = "현재 로그인 사용자의 회원 정보를 조회합니다."
    )
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserMeRes>> getMe(
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        UserMeRes response = userService.getMe(principal.userId());

        return ResponseEntity.ok(ApiResponse.ok("내 정보 조회가 완료되었습니다.", response));
    }

    @Operation(
        summary = "내 정보 수정",
        description = "현재 로그인 사용자의 회원 정보를 수정합니다."
    )
    @PatchMapping("/me")
    public ResponseEntity<ApiResponse<UserMeRes>> updateMe(
            @AuthenticationPrincipal AuthPrincipal principal,
            @Valid @RequestBody UpdateUserReq request
    ) {
        UserMeRes response = userService.updateMe(principal.userId(), request);

        return ResponseEntity.ok(ApiResponse.ok("내 정보 수정이 완료되었습니다.", response));
    }

}
