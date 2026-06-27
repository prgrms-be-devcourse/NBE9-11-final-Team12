package com.sisibibi.api.domain.usersanction.controller;

import com.sisibibi.api.domain.usersanction.dto.request.UserSanctionCreateReq;
import com.sisibibi.api.domain.usersanction.dto.request.UserSanctionExtendReq;
import com.sisibibi.api.domain.usersanction.dto.request.UserSanctionRevokeReq;
import com.sisibibi.api.domain.usersanction.dto.response.UserSanctionRes;
import com.sisibibi.api.domain.usersanction.dto.response.UserSanctionRecommendationRes;
import com.sisibibi.api.domain.usersanction.service.UserSanctionRecommendationService;
import com.sisibibi.api.domain.usersanction.service.UserSanctionService;
import com.sisibibi.api.global.response.ApiResponse;
import com.sisibibi.api.global.security.AuthPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "관리자 사용자 제재", description = "관리자 사용자 제재 등록, 조회, 추천, 해제, 연장 API")
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/users/{userId}/sanctions")
public class AdminUserSanctionController {

    private final UserSanctionService userSanctionService;
    private final UserSanctionRecommendationService recommendationService;

    @Operation(
        summary = "사용자 제재 등록",
        description = "관리자가 지정한 사용자에게 제재를 등록합니다."
    )
    @PostMapping
    public ResponseEntity<ApiResponse<UserSanctionRes>> createSanction(
            @PathVariable @Positive Long userId,
            @AuthenticationPrincipal AuthPrincipal principal,
            @Valid @RequestBody UserSanctionCreateReq request
    ) {
        UserSanctionRes response =
                userSanctionService.createSanction(userId, principal.userId(), request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.created("사용자 제재가 등록되었습니다.", response));
    }

    @Operation(
        summary = "사용자 제재 목록 조회",
        description = "관리자가 지정한 사용자의 제재 이력을 페이지 단위로 조회합니다."
    )
    @GetMapping
    public ResponseEntity<ApiResponse<Page<UserSanctionRes>>> getSanctions(
            @PathVariable @Positive Long userId,
            @PageableDefault(
                    size = 20,
                    sort = {"createdAt", "id"},
                    direction = Sort.Direction.DESC
            ) Pageable pageable
    ) {
        Page<UserSanctionRes> response = userSanctionService.getSanctions(userId, pageable);

        return ResponseEntity.ok(ApiResponse.ok("사용자 제재 목록 조회가 완료되었습니다.", response));
    }

    @Operation(
        summary = "사용자 제재 추천 조회",
        description = "신고 내용을 기준으로 지정한 사용자에게 적용할 제재 추천 정보를 조회합니다."
    )
    @GetMapping("/recommendation")
    public ResponseEntity<ApiResponse<UserSanctionRecommendationRes>> getRecommendation(
            @PathVariable @Positive Long userId,
            @RequestParam @Positive Long reportId
    ) {
        UserSanctionRecommendationRes response =
                recommendationService.recommend(userId, reportId);

        return ResponseEntity.ok(ApiResponse.ok(
                "사용자 제재 추천 조회가 완료되었습니다.",
                response
        ));
    }

    @Operation(
        summary = "사용자 제재 해제",
        description = "관리자가 지정한 사용자의 제재를 해제합니다."
    )
    @PatchMapping("/{sanctionId}/revoke")
    public ResponseEntity<ApiResponse<UserSanctionRes>> revokeSanction(
            @PathVariable @Positive Long userId,
            @PathVariable @Positive Long sanctionId,
            @AuthenticationPrincipal AuthPrincipal principal,
            @Valid @RequestBody UserSanctionRevokeReq request
    ) {
        UserSanctionRes response = userSanctionService.revokeSanction(
                userId,
                sanctionId,
                principal.userId(),
                request.reason()
        );

        return ResponseEntity.ok(ApiResponse.ok("사용자 제재가 해제되었습니다.", response));
    }

    @Operation(
        summary = "사용자 제재 연장",
        description = "관리자가 지정한 사용자의 제재 기간을 연장합니다."
    )
    @PatchMapping("/{sanctionId}/extend")
    public ResponseEntity<ApiResponse<UserSanctionRes>> extendSanction(
            @PathVariable @Positive Long userId,
            @PathVariable @Positive Long sanctionId,
            @AuthenticationPrincipal AuthPrincipal principal,
            @Valid @RequestBody UserSanctionExtendReq request
    ) {
        UserSanctionRes response = userSanctionService.extendSanction(
                userId,
                sanctionId,
                principal.userId(),
                request.durationHours(),
                request.reason()
        );

        return ResponseEntity.ok(ApiResponse.ok("사용자 제재가 연장되었습니다.", response));
    }
}
