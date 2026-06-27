package com.sisibibi.api.domain.usersanction.controller;

import com.sisibibi.api.domain.usersanction.dto.response.ActiveUserSanctionRes;
import com.sisibibi.api.domain.usersanction.service.UserSanctionService;
import com.sisibibi.api.global.response.ApiResponse;
import com.sisibibi.api.global.security.AuthPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "사용자 제재", description = "내 활성 제재 조회 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users/me/sanctions")
public class UserSanctionController {

    private final UserSanctionService userSanctionService;

    @Operation(
        summary = "내 활성 제재 조회",
        description = "현재 로그인 사용자의 활성 상태 제재 목록을 조회합니다."
    )
    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<ActiveUserSanctionRes>>> getActiveSanctions(
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        List<ActiveUserSanctionRes> response =
                userSanctionService.getActiveSanctions(principal.userId());

        return ResponseEntity.ok(ApiResponse.ok(
                "활성 사용자 제재 조회가 완료되었습니다.",
                response
        ));
    }
}
