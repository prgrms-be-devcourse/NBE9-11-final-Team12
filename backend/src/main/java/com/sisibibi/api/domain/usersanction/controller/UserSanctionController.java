package com.sisibibi.api.domain.usersanction.controller;

import com.sisibibi.api.domain.usersanction.dto.response.ActiveUserSanctionRes;
import com.sisibibi.api.domain.usersanction.service.UserSanctionService;
import com.sisibibi.api.global.response.ApiResponse;
import com.sisibibi.api.global.security.AuthPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users/me/sanctions")
public class UserSanctionController {

    private final UserSanctionService userSanctionService;

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
