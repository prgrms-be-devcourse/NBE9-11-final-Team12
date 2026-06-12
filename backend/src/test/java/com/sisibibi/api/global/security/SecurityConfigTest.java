package com.sisibibi.api.global.security;

import com.sisibibi.api.ApiApplication;
import com.sisibibi.api.global.response.ApiResponse;
import com.sisibibi.api.global.security.cookie.AuthCookieProvider;
import com.sisibibi.api.global.security.jwt.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.Cookie;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = ApiApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(SecurityConfigTest.TestProtectedController.class)
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Test
    void protectedApi_returnsApiResponse401WithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/test/protected"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.code", is("UNAUTHORIZED")))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void protectedApi_acceptsValidAccessTokenCookie() throws Exception {
        String accessToken = jwtTokenProvider.createAccessToken(
                new AuthPrincipal(1L, "user@example.com", "USER")
        );

        mockMvc.perform(get("/api/v1/test/protected")
                        .cookie(new Cookie(AuthCookieProvider.ACCESS_TOKEN_COOKIE_NAME, accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(200)))
                .andExpect(jsonPath("$.data", is(1)));
    }

    @Test
    void cors_allowsConfiguredFrontendOriginWithCredentials() throws Exception {
        mockMvc.perform(options("/api/v1/test/protected")
                        .header("Origin", "http://localhost:3000")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
    }

    @RestController
    static class TestProtectedController {

        @GetMapping("/api/v1/test/protected")
        ResponseEntity<ApiResponse<Long>> protectedApi(
                @AuthenticationPrincipal AuthPrincipal principal
        ) {
            return ResponseEntity.ok(ApiResponse.ok(principal.userId()));
        }
    }
}
