package com.sisibibi.api.domain.speech.loadtest;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LoadTestStageExpirationController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("load-test")
class LoadTestStageExpirationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LoadTestStageExpirationService loadTestStageExpirationService;

    @Test
    void prepareExpirationCandidates_returnsPreparedCounts() throws Exception {
        given(loadTestStageExpirationService.prepareExpirationCandidates(
                10,
                2,
                1L,
                1L
        )).willReturn(new LoadTestExpirationPrepareRes(
                1L,
                1L,
                10,
                2,
                10,
                20
        ));

        mockMvc.perform(post("/api/load-test/stage/expiration/prepare")
                        .param("roomCount", "10")
                        .param("waitingPerRoom", "2")
                        .param("roomIdStart", "1")
                        .param("userIdStart", "1"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.roomIdStart").value(1))
                .andExpect(jsonPath("$.data.userIdStart").value(1))
                .andExpect(jsonPath("$.data.roomCount").value(10))
                .andExpect(jsonPath("$.data.waitingPerRoom").value(2))
                .andExpect(jsonPath("$.data.preparedCurrentSpeakers").value(10))
                .andExpect(jsonPath("$.data.preparedWaitingSpeakers").value(20));
    }

    @Test
    void runExpiration_returnsProcessingMetrics() throws Exception {
        given(loadTestStageExpirationService.runExpiration())
                .willReturn(new LoadTestExpirationRunRes(
                        10,
                        10,
                        0,
                        123,
                        12
                ));

        mockMvc.perform(post("/api/load-test/stage/expiration/run"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.candidateRoomCount").value(10))
                .andExpect(jsonPath("$.data.expiredCount").value(10))
                .andExpect(jsonPath("$.data.failureCount").value(0))
                .andExpect(jsonPath("$.data.elapsedMs").value(123))
                .andExpect(jsonPath("$.data.avgPerRoomMs").value(12));
    }

    @Test
    void prepareExpirationRace_returnsPreparedFixture() throws Exception {
        given(loadTestStageExpirationService.prepareExpirationRace(
                1L,
                10L,
                11L
        )).willReturn(new LoadTestExpirationRacePrepareRes(
                1L,
                10L,
                11L,
                1,
                1
        ));

        mockMvc.perform(post("/api/load-test/stage/expiration/race/prepare")
                        .param("roomId", "1")
                        .param("currentSpeakerUserId", "10")
                        .param("nextSpeakerUserId", "11"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.roomId").value(1))
                .andExpect(jsonPath("$.data.currentSpeakerUserId").value(10))
                .andExpect(jsonPath("$.data.nextSpeakerUserId").value(11))
                .andExpect(jsonPath("$.data.preparedCurrentSpeakers").value(1))
                .andExpect(jsonPath("$.data.preparedWaitingSpeakers").value(1));
    }

    @Test
    void verifyExpirationRace_returnsFinalStateCounts() throws Exception {
        given(loadTestStageExpirationService.verifyExpirationRace(1L))
                .willReturn(new LoadTestExpirationRaceVerifyRes(
                        1L,
                        1,
                        0,
                        1,
                        1,
                        0,
                        true
                ));

        mockMvc.perform(post("/api/load-test/stage/expiration/race/verify")
                        .param("roomId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.roomId").value(1))
                .andExpect(jsonPath("$.data.terminalCount").value(1))
                .andExpect(jsonPath("$.data.completedCount").value(0))
                .andExpect(jsonPath("$.data.expiredCount").value(1))
                .andExpect(jsonPath("$.data.assignedCount").value(1))
                .andExpect(jsonPath("$.data.waitingCount").value(0))
                .andExpect(jsonPath("$.data.valid").value(true));
    }
}
