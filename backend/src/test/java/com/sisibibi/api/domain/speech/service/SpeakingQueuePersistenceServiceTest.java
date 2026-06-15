package com.sisibibi.api.domain.speech.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.sisibibi.api.domain.speech.entity.SpeakingQueue;
import com.sisibibi.api.domain.speech.entity.SpeakingQueueStatus;
import com.sisibibi.api.domain.speech.repository.SpeakingQueueRepository;
import com.sisibibi.api.global.exception.CustomException;
import com.sisibibi.api.global.exception.ErrorCode;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class SpeakingQueuePersistenceServiceTest {

    @Mock
    private SpeakingQueueRepository speakingQueueRepository;

    @InjectMocks
    private SpeakingQueuePersistenceService speakingQueuePersistenceService;

    @Test
    void createWaitingRequest_persistsRequestAndAssignsOrderFromId() {
        given(speakingQueueRepository.existsByRoomIdAndUserIdAndStatusIn(
                1L,
                7L,
                List.of(SpeakingQueueStatus.WAITING, SpeakingQueueStatus.ASSIGNED)
        )).willReturn(false);
        given(speakingQueueRepository.saveAndFlush(any(SpeakingQueue.class)))
                .willAnswer(invocation -> {
                    SpeakingQueue saved = invocation.getArgument(0);
                    ReflectionTestUtils.setField(saved, "id", 15L);
                    return saved;
                });

        SpeakingQueue saved =
                speakingQueuePersistenceService.createWaitingRequest(1L, 7L);

        assertThat(saved.getQueueOrder()).isEqualTo(15);
        assertThat(saved.getStatus()).isEqualTo(SpeakingQueueStatus.WAITING);
    }

    @Test
    void createWaitingRequest_rejectsExistingActiveRequest() {
        given(speakingQueueRepository.existsByRoomIdAndUserIdAndStatusIn(
                1L,
                7L,
                List.of(SpeakingQueueStatus.WAITING, SpeakingQueueStatus.ASSIGNED)
        )).willReturn(true);

        assertThatThrownBy(() ->
                speakingQueuePersistenceService.createWaitingRequest(1L, 7L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.SPEAKING_REQUEST_ALREADY_EXISTS);

        verify(speakingQueueRepository, never()).saveAndFlush(any(SpeakingQueue.class));
    }
}
