package com.sisibibi.api.domain.usersanction.controller;

import com.sisibibi.api.domain.usersanction.dto.request.UserSanctionCreateReq;
import com.sisibibi.api.domain.usersanction.dto.request.UserSanctionRevokeReq;
import com.sisibibi.api.domain.usersanction.dto.response.UserSanctionRes;
import com.sisibibi.api.domain.usersanction.dto.response.UserSanctionRecommendationRes;
import com.sisibibi.api.domain.usersanction.service.UserSanctionRecommendationService;
import com.sisibibi.api.domain.usersanction.service.UserSanctionService;
import com.sisibibi.api.global.response.ApiResponse;
import com.sisibibi.api.global.security.AuthPrincipal;
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

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/users/{userId}/sanctions")
public class AdminUserSanctionController {

    private final UserSanctionService userSanctionService;
    private final UserSanctionRecommendationService recommendationService;

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
}
