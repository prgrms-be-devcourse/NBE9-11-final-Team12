package com.sisibibi.api.global.security;

import com.sisibibi.api.ApiApplication;
import com.sisibibi.api.domain.report.prompt.PromptGuardResult;
import com.sisibibi.api.domain.report.prompt.PromptGuardService;
import com.sisibibi.api.domain.report.prompt.PromptSeverity;
import com.sisibibi.api.global.response.ApiResponse;
import com.sisibibi.api.global.security.cookie.AuthCookieProvider;
import com.sisibibi.api.global.security.jwt.JwtTokenProvider;
import com.sisibibi.api.global.security.session.TokenSessionValidator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.Cookie;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import org.springframework.http.MediaType;

@SpringBootTest(classes = ApiApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({
        SecurityConfigTest.TestProtectedController.class,
        SecurityConfigTest.TestInternalController.class
})
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private TokenSessionValidator tokenSessionValidator;

    @MockitoBean
    private PromptGuardService promptGuardService;

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
    void adminApi_rejectsUserRole() throws Exception {
        String accessToken = jwtTokenProvider.createAccessToken(
                new AuthPrincipal(1L, "user@example.com", "USER")
        );

        mockMvc.perform(get("/api/v1/admin/test")
                        .cookie(new Cookie(AuthCookieProvider.ACCESS_TOKEN_COOKIE_NAME, accessToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminApi_acceptsAdminRole() throws Exception {
        String accessToken = jwtTokenProvider.createAccessToken(
                new AuthPrincipal(1L, "admin@example.com", "ADMIN")
        );

        mockMvc.perform(get("/api/v1/admin/test")
                        .cookie(new Cookie(AuthCookieProvider.ACCESS_TOKEN_COOKIE_NAME, accessToken)))
                .andExpect(status().isOk());
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

    @Test
    void signup_isPublicAndDoesNotRequireCsrfToken() throws Exception {
        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "security-signup@example.com",
                                  "password": "password123!",
                                  "nickname": "tester"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code", is("SUCCESS")))
                .andExpect(jsonPath("$.data.email", is("security-signup@example.com")));
    }

    @Test
    void authApi_isPublicAndDoesNotRequireCsrfToken() throws Exception {
        mockMvc.perform(post("/api/v1/auth/test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is("SUCCESS")));
    }

    @Test
    void internalApi_isPublicAndDoesNotRequireCsrfToken() throws Exception {
        mockMvc.perform(post("/api/v1/internal/test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is("SUCCESS")));
    }

    @Test
    void actuatorHealth_isPublic() throws Exception {
        given(promptGuardService.scan("prompt guard health check"))
                .willReturn(PromptGuardResult.allowed(PromptSeverity.SAFE, "test health"));

        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("UP")));
    }

    @Test
    void actuatorPrometheus_isPublic() throws Exception {
        mockMvc.perform(get("/actuator/prometheus"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("jvm_memory_used_bytes")));
    }

    @Test
    void swaggerUiAndOpenApiDocs_arePublic() throws Exception {
        mockMvc.perform(get("/swagger-ui.html"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", containsString("/swagger-ui/index.html")));

        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openapi").exists())
                .andExpect(jsonPath("$.info.title", is("Sisibibi API")));
    }

    @Test
    void roomListAndOpenRooms_arePublic() throws Exception {
        mockMvc.perform(get("/api/v1/rooms"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/rooms/open"))
                .andExpect(status().isOk());
    }

    @Test
    void roomDetailAndParticipantCount_arePublic() throws Exception {
        mockMvc.perform(get("/api/v1/rooms/999999"))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/v1/rooms/999999/participants/count"))
                .andExpect(status().isNotFound());
    }

    @Test
    void participationChatStageAndParticipantList_stayProtected() throws Exception {
        mockMvc.perform(post("/api/v1/rooms/1/participants").with(csrf()))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/rooms/1/participants"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/rooms/1/chat/messages"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/rooms/1/speeches"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/speeches/1"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/rooms/1/stage"))
                .andExpect(status().isUnauthorized());
    }

    @RestController
    static class TestProtectedController {

        @GetMapping("/api/v1/test/protected")
        ResponseEntity<ApiResponse<Long>> protectedApi(
                @AuthenticationPrincipal AuthPrincipal principal
        ) {
            return ResponseEntity.ok(ApiResponse.ok(principal.userId()));
        }

        @GetMapping("/api/v1/admin/test")
        ResponseEntity<ApiResponse<Long>> adminApi(
                @AuthenticationPrincipal AuthPrincipal principal
        ) {
            return ResponseEntity.ok(ApiResponse.ok(principal.userId()));
        }

        @PostMapping("/api/v1/auth/test")
        ResponseEntity<ApiResponse<Void>> publicAuthApi() {
            return ResponseEntity.ok(ApiResponse.okMessage("인증 공개 API입니다."));
        }
    }

    @RestController
    static class TestInternalController {

        @PostMapping("/api/v1/internal/test")
        ResponseEntity<ApiResponse<Void>> publicInternalApi() {
            return ResponseEntity.ok(ApiResponse.okMessage("internal API"));
        }
    }
}
