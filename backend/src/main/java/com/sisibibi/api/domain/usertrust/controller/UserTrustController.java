package com.sisibibi.api.domain.usertrust.controller;

import com.sisibibi.api.domain.usertrust.dto.response.UserTrustDetailRes;
import com.sisibibi.api.domain.usertrust.dto.response.UserTrustSummaryRes;
import com.sisibibi.api.domain.usertrust.service.UserTrustService;
import com.sisibibi.api.global.response.ApiResponse;
import com.sisibibi.api.global.security.AuthPrincipal;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
public class UserTrustController {

    private final UserTrustService userTrustService;

    @GetMapping("/me/trust")
    public ResponseEntity<ApiResponse<UserTrustDetailRes>> getMyTrust(
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                "내 신뢰도 조회가 완료되었습니다.",
                userTrustService.getMyTrust(principal.userId())
        ));
    }

    @GetMapping("/{userId}/trust")
    public ResponseEntity<ApiResponse<UserTrustSummaryRes>> getUserTrust(
            @PathVariable @Positive Long userId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                "사용자 신뢰도 조회가 완료되었습니다.",
                userTrustService.getUserTrust(userId)
        ));
    }
}
