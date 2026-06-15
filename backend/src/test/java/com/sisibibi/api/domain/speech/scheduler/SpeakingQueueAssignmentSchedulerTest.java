package com.sisibibi.api.domain.speech.scheduler;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.sisibibi.api.domain.speech.repository.SpeakingQueueRepository;
import com.sisibibi.api.domain.speech.service.SpeakingQueueService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SpeakingQueueAssignmentSchedulerTest {

    @Mock
    private SpeakingQueueRepository speakingQueueRepository;

    @Mock
    private SpeakingQueueService speakingQueueService;

    @InjectMocks
    private SpeakingQueueAssignmentScheduler speakingQueueAssignmentScheduler;

    @Test
    void assignWaitingSpeakers_requestsAssignmentForEachCandidateRoom() {
        given(speakingQueueRepository.findRoomIdsRequiringAssignment())
                .willReturn(List.of(1L, 2L));

        speakingQueueAssignmentScheduler.assignWaitingSpeakers();

        verify(speakingQueueService).assignNextSpeaker(1L);
        verify(speakingQueueService).assignNextSpeaker(2L);
    }

    @Test
    void assignWaitingSpeakers_doesNothingWhenThereIsNoCandidateRoom() {
        given(speakingQueueRepository.findRoomIdsRequiringAssignment())
                .willReturn(List.of());

        speakingQueueAssignmentScheduler.assignWaitingSpeakers();

        verify(speakingQueueService, never())
                .assignNextSpeaker(org.mockito.ArgumentMatchers.anyLong());
    }
}
