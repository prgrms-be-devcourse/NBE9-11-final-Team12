package com.sisibibi.api.domain.speech.scheduler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.sisibibi.api.domain.room.repository.RoomRepository;
import com.sisibibi.api.domain.speech.config.StageSummaryProperties;
import com.sisibibi.api.domain.speech.entity.StageSummary;
import com.sisibibi.api.domain.speech.repository.StageSummaryRepository;
import com.sisibibi.api.domain.speech.service.StageSummaryService;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class StageSummarySchedulerTest {

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private StageSummaryRepository stageSummaryRepository;

    @Mock
    private StageSummaryService stageSummaryService;

    private StageSummaryProperties stageSummaryProperties;
    private StageSummaryScheduler stageSummaryScheduler;

    @BeforeEach
    void setUp() {
        stageSummaryProperties = new StageSummaryProperties();
        stageSummaryProperties.setEnabled(true);
        stageSummaryProperties.setMaxGenerationAttempts(3);
        stageSummaryProperties.setRecoveryBatchSize(20);
        stageSummaryScheduler = new StageSummaryScheduler(
                roomRepository,
                stageSummaryRepository,
                stageSummaryService,
                stageSummaryProperties
        );
    }

    @Test
    void recoverMissedSummariesAndRetries_generatesForCandidateRoomsAndFailedSummaries() {
        StageSummary failed = StageSummary.pending(
                3L,
                LocalDateTime.of(2026, 6, 26, 11, 0),
                10
        );
        ReflectionTestUtils.setField(failed, "id", 77L);
        failed.fail("temporary failure");
        given(roomRepository.findStageSummaryCandidateRoomIds(any(LocalDateTime.class), any()))
                .willReturn(List.of(1L, 2L));
        given(stageSummaryRepository.findRetryCandidates(any(), any(Integer.class), any()))
                .willReturn(List.of(failed));

        stageSummaryScheduler.recoverMissedSummariesAndRetries();

        verify(stageSummaryService).generateIfNeeded(1L);
        verify(stageSummaryService).generateIfNeeded(2L);
        verify(stageSummaryService).generateIfNeeded(3L);
    }

    @Test
    void recoverMissedSummariesAndRetries_doesNothingWhenDisabled() {
        stageSummaryProperties.setEnabled(false);

        stageSummaryScheduler.recoverMissedSummariesAndRetries();

        verify(roomRepository, never()).findStageSummaryCandidateRoomIds(any(LocalDateTime.class), any());
        verify(stageSummaryRepository, never()).findRetryCandidates(any(), any(Integer.class), any());
        verify(stageSummaryService, never()).generateIfNeeded(any());
    }
}
