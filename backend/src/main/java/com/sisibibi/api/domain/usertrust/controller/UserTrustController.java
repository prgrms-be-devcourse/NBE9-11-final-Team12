package com.sisibibi.api.domain.usertrust.controller;

import com.sisibibi.api.domain.usertrust.dto.response.UserTrustDetailRes;
import com.sisibibi.api.domain.usertrust.dto.response.UserTrustSummaryRes;
import com.sisibibi.api.domain.usertrust.service.UserTrustService;
import com.sisibibi.api.global.response.ApiResponse;
import com.sisibibi.api.global.security.AuthPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "사용자 신뢰도", description = "내 신뢰도 및 사용자 신뢰도 조회 API")
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
public class UserTrustController {

    private final UserTrustService userTrustService;

    @Operation(
        summary = "내 신뢰도 조회",
        description = "현재 로그인 사용자의 신뢰도 상세 정보를 조회합니다."
    )
    @GetMapping("/me/trust")
    public ResponseEntity<ApiResponse<UserTrustDetailRes>> getMyTrust(
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                "내 신뢰도 조회가 완료되었습니다.",
                userTrustService.getMyTrust(principal.userId())
        ));
    }

    @Operation(
        summary = "사용자 신뢰도 조회",
        description = "지정한 사용자의 공개 신뢰도 요약 정보를 조회합니다."
    )
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
