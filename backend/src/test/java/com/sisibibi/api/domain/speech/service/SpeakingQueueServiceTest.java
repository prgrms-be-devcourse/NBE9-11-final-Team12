package com.sisibibi.api.domain.speech.service;

import com.sisibibi.api.domain.speech.dto.response.StageQueueRes;
import com.sisibibi.api.domain.speech.dto.response.StageRequestRes;
import com.sisibibi.api.domain.speech.dto.response.CurrentSpeakerRes;
import com.sisibibi.api.domain.speech.entity.SpeakingQueueStatus;
import com.sisibibi.api.global.exception.CustomException;
import com.sisibibi.api.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class SpeakingQueueServiceTest {

    @Autowired
    private SpeakingQueueService speakingQueueService;

    @Test
    void requestSpeakingTurn_assignsNextQueueOrderInRoom() {
        StageRequestRes first = speakingQueueService.requestSpeakingTurn(1L, 10L);
        StageRequestRes second = speakingQueueService.requestSpeakingTurn(1L, 20L);

        assertThat(first.queueOrder()).isEqualTo(1);
        assertThat(second.queueOrder()).isEqualTo(2);
        assertThat(first.status()).isEqualTo(SpeakingQueueStatus.WAITING);
        assertThat(second.status()).isEqualTo(SpeakingQueueStatus.WAITING);
    }

    @Test
    void requestSpeakingTurn_rejectsDuplicateActiveRequestInSameRoom() {
        speakingQueueService.requestSpeakingTurn(2L, 10L);

        assertThatThrownBy(() -> speakingQueueService.requestSpeakingTurn(2L, 10L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.SPEAKING_REQUEST_ALREADY_EXISTS);
    }

    @Test
    void cancelMyRequest_marksWaitingRequestCanceled() {
        speakingQueueService.requestSpeakingTurn(3L, 10L);

        speakingQueueService.cancelMyRequest(3L, 10L);

        StageRequestRes canceled = speakingQueueService.getMyRequest(3L, 10L);
        assertThat(canceled.status()).isEqualTo(SpeakingQueueStatus.CANCELED);
    }

    @Test
    void cancelMyRequest_throwsWhenWaitingRequestDoesNotExist() {
        assertThatThrownBy(() -> speakingQueueService.cancelMyRequest(5L, 10L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.SPEAKING_REQUEST_NOT_FOUND);
    }

    @Test
    void getMyRequest_throwsWhenRequestDoesNotExist() {
        assertThatThrownBy(() -> speakingQueueService.getMyRequest(6L, 10L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.SPEAKING_REQUEST_NOT_FOUND);
    }

    @Test
    void getWaitingQueue_returnsWaitingRequestsOrderedByQueueOrder() {
        speakingQueueService.requestSpeakingTurn(4L, 30L);
        speakingQueueService.requestSpeakingTurn(4L, 10L);
        speakingQueueService.requestSpeakingTurn(4L, 20L);
        speakingQueueService.cancelMyRequest(4L, 10L);

        StageQueueRes queue = speakingQueueService.getWaitingQueue(4L);

        assertThat(queue.roomId()).isEqualTo(4L);
        assertThat(queue.items())
                .extracting(StageQueueRes.StageQueueItemRes::userId)
                .isEqualTo(List.of(30L, 20L));
        assertThat(queue.items())
                .extracting(StageQueueRes.StageQueueItemRes::queueOrder)
                .isEqualTo(List.of(1, 3));
    }

    @Test
    void assignNextSpeaker_assignsFirstWaitingRequest() {
        speakingQueueService.requestSpeakingTurn(7L, 30L);
        speakingQueueService.requestSpeakingTurn(7L, 10L);

        CurrentSpeakerRes currentSpeaker = speakingQueueService.assignNextSpeaker(7L);

        assertThat(currentSpeaker.roomId()).isEqualTo(7L);
        assertThat(currentSpeaker.userId()).isEqualTo(30L);
        assertThat(currentSpeaker.queueOrder()).isEqualTo(1);
        assertThat(currentSpeaker.status()).isEqualTo(SpeakingQueueStatus.ASSIGNED);

        StageQueueRes waitingQueue = speakingQueueService.getWaitingQueue(7L);
        assertThat(waitingQueue.items())
                .extracting(StageQueueRes.StageQueueItemRes::userId)
                .isEqualTo(List.of(10L));
    }

    @Test
    void assignNextSpeaker_throwsWhenCurrentSpeakerAlreadyExists() {
        speakingQueueService.requestSpeakingTurn(8L, 10L);
        speakingQueueService.requestSpeakingTurn(8L, 20L);
        speakingQueueService.assignNextSpeaker(8L);

        assertThatThrownBy(() -> speakingQueueService.assignNextSpeaker(8L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.CURRENT_SPEAKER_ALREADY_EXISTS);
    }

    @Test
    void assignNextSpeaker_throwsWhenWaitingQueueIsEmpty() {
        assertThatThrownBy(() -> speakingQueueService.assignNextSpeaker(9L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.SPEAKING_QUEUE_EMPTY);
    }

    @Test
    void getCurrentSpeaker_returnsAssignedRequest() {
        speakingQueueService.requestSpeakingTurn(10L, 10L);
        speakingQueueService.assignNextSpeaker(10L);

        CurrentSpeakerRes currentSpeaker = speakingQueueService.getCurrentSpeaker(10L);

        assertThat(currentSpeaker.userId()).isEqualTo(10L);
        assertThat(currentSpeaker.status()).isEqualTo(SpeakingQueueStatus.ASSIGNED);
    }

    @Test
    void getCurrentSpeaker_throwsWhenCurrentSpeakerDoesNotExist() {
        assertThatThrownBy(() -> speakingQueueService.getCurrentSpeaker(11L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.CURRENT_SPEAKER_NOT_FOUND);
    }
}
