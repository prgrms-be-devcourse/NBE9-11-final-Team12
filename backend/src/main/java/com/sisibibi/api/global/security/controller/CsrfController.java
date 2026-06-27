package com.sisibibi.api.global.security.controller;

import com.sisibibi.api.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "보안", description = "CSRF 토큰 발급 API")
@RestController
@RequestMapping("/api/v1/csrf")
public class CsrfController {

    @Operation(
        summary = "CSRF 토큰 발급",
        description = "상태 변경 요청에 사용할 CSRF 토큰을 발급합니다. 응답 본문과 XSRF-TOKEN 쿠키를 통해 토큰을 확인할 수 있습니다."
    )
    @GetMapping
    public ResponseEntity<ApiResponse<String>> issue(CsrfToken csrfToken) {
        return ResponseEntity.ok(ApiResponse.ok(
                "CSRF 토큰이 발급되었습니다.",
                csrfToken.getToken()
        ));
    }
}
