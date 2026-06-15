package com.sisibibi.api.global.security.controller;

import com.sisibibi.api.global.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/csrf")
public class CsrfController {

    @GetMapping
    public ResponseEntity<ApiResponse<String>> issue(CsrfToken csrfToken) {
        return ResponseEntity.ok(ApiResponse.ok(
                "CSRF 토큰이 발급되었습니다.",
                csrfToken.getToken()
        ));
    }
}
