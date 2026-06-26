package com.sisibibi.api.domain.usersanction.controller;

import com.sisibibi.api.ApiApplication;
import com.sisibibi.api.domain.speechreport.entity.ViolationSeverity;
import com.sisibibi.api.domain.usersanction.dto.response.UserSanctionRecommendationRes;
import com.sisibibi.api.domain.usersanction.entity.UserSanctionType;
import com.sisibibi.api.domain.usersanction.service.UserSanctionRecommendationService;
import com.sisibibi.api.global.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminUserSanctionController.class)
@AutoConfigureMockMvc(addFilters = false)
@ContextConfiguration(classes = {
        ApiApplication.class,
        AdminUserSanctionController.class,
        GlobalExceptionHandler.class
})
class AdminUserSanctionRecommendationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private com.sisibibi.api.domain.usersanction.service.UserSanctionService userSanctionService;

    @MockitoBean
    private UserSanctionRecommendationService recommendationService;

    @Test
    void getRecommendation_returnsPolicyResult() throws Exception {
        given(recommendationService.recommend(10L, 100L))
                .willReturn(new UserSanctionRecommendationRes(
                        100L,
                        10L,
                        ViolationSeverity.MEDIUM,
                        90,
                        2,
                        0,
                        2,
                        0,
                        0,
                        4,
                        UserSanctionType.SPEECH_RESTRICTION,
                        24,
                        false,
                        false,
                        null,
                        null,
                        "최근 90일 누적 위반 점수가 4점 이상입니다."
                ));

        mockMvc.perform(get(
                        "/api/v1/admin/users/{userId}/sanctions/recommendation",
                        10L
                ).queryParam("reportId", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.recommendedType")
                        .value("SPEECH_RESTRICTION"))
                .andExpect(jsonPath("$.data.recommendedDurationHours").value(24))
                .andExpect(jsonPath("$.data.accountSuspensionReviewRecommended").value(false))
                .andExpect(jsonPath("$.data.mediumCount").value(2))
                .andExpect(jsonPath("$.data.activeSameTypeSanction").value(false));
    }
}
