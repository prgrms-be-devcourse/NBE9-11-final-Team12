package com.sisibibi.api.domain.speech.service;

import com.sisibibi.api.domain.room.entity.Room;
import com.sisibibi.api.domain.room.repository.RoomRepository;
import com.sisibibi.api.domain.speech.dto.response.StageExpireRes;
import com.sisibibi.api.domain.speech.dto.response.StageQueueRes;
import com.sisibibi.api.domain.speech.dto.response.StageCompleteRes;
import com.sisibibi.api.domain.speech.dto.response.StageRequestRes;
import com.sisibibi.api.domain.speech.dto.response.CurrentSpeakerRes;
import com.sisibibi.api.domain.speech.entity.SpeakingQueueStatus;
import com.sisibibi.api.domain.user.entity.User;
import com.sisibibi.api.domain.user.repository.UserRepository;
import com.sisibibi.api.global.exception.CustomException;
import com.sisibibi.api.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class SpeakingQueueServiceTest {

    @Autowired
    private SpeakingQueueService speakingQueueService;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void requestSpeakingTurn_assignsNextQueueOrderInRoom() {
        Long roomId = createOpenRoom();
        Long firstUserId = createActiveUser();
        Long secondUserId = createActiveUser();

        StageRequestRes first = speakingQueueService.requestSpeakingTurn(roomId, firstUserId);
        StageRequestRes second = speakingQueueService.requestSpeakingTurn(roomId, secondUserId);

        assertThat(first.queueOrder()).isEqualTo(1);
        assertThat(second.queueOrder()).isEqualTo(2);
        assertThat(first.status()).isEqualTo(SpeakingQueueStatus.WAITING);
        assertThat(second.status()).isEqualTo(SpeakingQueueStatus.WAITING);
    }

    @Test
    void requestSpeakingTurn_rejectsDuplicateActiveRequestInSameRoom() {
        Long roomId = createOpenRoom();
        Long userId = createActiveUser();

        speakingQueueService.requestSpeakingTurn(roomId, userId);

        assertThatThrownBy(() -> speakingQueueService.requestSpeakingTurn(roomId, userId))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.SPEAKING_REQUEST_ALREADY_EXISTS);
    }

    @Test
    void requestSpeakingTurn_allowsSameUserToRequestInDifferentRooms() {
        Long firstRoomId = createOpenRoom();
        Long secondRoomId = createOpenRoom();
        Long userId = createActiveUser();

        StageRequestRes first = speakingQueueService.requestSpeakingTurn(firstRoomId, userId);
        StageRequestRes second = speakingQueueService.requestSpeakingTurn(secondRoomId, userId);

        assertThat(first.roomId()).isEqualTo(firstRoomId);
        assertThat(second.roomId()).isEqualTo(secondRoomId);
        assertThat(first.userId()).isEqualTo(userId);
        assertThat(second.userId()).isEqualTo(userId);
        assertThat(first.queueOrder()).isEqualTo(1);
        assertThat(second.queueOrder()).isEqualTo(1);
    }

    @Test
    void requestSpeakingTurn_allowsRequestAgainAfterCancelingWaitingRequest() {
        Long roomId = createOpenRoom();
        Long userId = createActiveUser();

        StageRequestRes first = speakingQueueService.requestSpeakingTurn(roomId, userId);
        speakingQueueService.cancelMyRequest(roomId, userId);

        StageRequestRes second = speakingQueueService.requestSpeakingTurn(roomId, userId);

        assertThat(first.status()).isEqualTo(SpeakingQueueStatus.WAITING);
        assertThat(second.status()).isEqualTo(SpeakingQueueStatus.WAITING);
        assertThat(second.queueOrder()).isEqualTo(2);
    }

    @Test
    void requestSpeakingTurn_rejectsDuplicateRequestWhenUserAlreadyAssigned() {
        Long roomId = createOpenRoom();
        Long userId = createActiveUser();

        speakingQueueService.requestSpeakingTurn(roomId, userId);
        speakingQueueService.assignNextSpeaker(roomId);

        assertThatThrownBy(() -> speakingQueueService.requestSpeakingTurn(roomId, userId))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.SPEAKING_REQUEST_ALREADY_EXISTS);
    }

    @Test
    void cancelMyRequest_marksWaitingRequestCanceled() {
        Long roomId = createOpenRoom();
        Long userId = createActiveUser();

        speakingQueueService.requestSpeakingTurn(roomId, userId);

        speakingQueueService.cancelMyRequest(roomId, userId);

        StageRequestRes canceled = speakingQueueService.getMyRequest(roomId, userId);
        assertThat(canceled.status()).isEqualTo(SpeakingQueueStatus.CANCELED);
    }

    @Test
    void cancelMyRequest_throwsWhenWaitingRequestDoesNotExist() {
        Long roomId = createOpenRoom();
        Long userId = createActiveUser();

        assertThatThrownBy(() -> speakingQueueService.cancelMyRequest(roomId, userId))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.SPEAKING_REQUEST_NOT_FOUND);
    }

    @Test
    void getMyRequest_throwsWhenRequestDoesNotExist() {
        Long roomId = createOpenRoom();
        Long userId = createActiveUser();

        assertThatThrownBy(() -> speakingQueueService.getMyRequest(roomId, userId))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.SPEAKING_REQUEST_NOT_FOUND);
    }

    @Test
    void getWaitingQueue_returnsWaitingRequestsOrderedByQueueOrder() {
        Long roomId = createOpenRoom();
        Long firstUserId = createActiveUser();
        Long secondUserId = createActiveUser();
        Long thirdUserId = createActiveUser();

        speakingQueueService.requestSpeakingTurn(roomId, firstUserId);
        speakingQueueService.requestSpeakingTurn(roomId, secondUserId);
        speakingQueueService.requestSpeakingTurn(roomId, thirdUserId);
        speakingQueueService.cancelMyRequest(roomId, secondUserId);

        StageQueueRes queue = speakingQueueService.getWaitingQueue(roomId);

        assertThat(queue.roomId()).isEqualTo(roomId);
        assertThat(queue.items())
                .extracting(StageQueueRes.StageQueueItemRes::userId)
                .isEqualTo(List.of(firstUserId, thirdUserId));
        assertThat(queue.items())
                .extracting(StageQueueRes.StageQueueItemRes::queueOrder)
                .isEqualTo(List.of(1, 3));
    }

    @Test
    void assignNextSpeaker_assignsFirstWaitingRequest() {
        Long roomId = createOpenRoom();
        Long firstUserId = createActiveUser();
        Long secondUserId = createActiveUser();

        speakingQueueService.requestSpeakingTurn(roomId, firstUserId);
        speakingQueueService.requestSpeakingTurn(roomId, secondUserId);

        CurrentSpeakerRes currentSpeaker = speakingQueueService.assignNextSpeaker(roomId);

        assertThat(currentSpeaker.roomId()).isEqualTo(roomId);
        assertThat(currentSpeaker.userId()).isEqualTo(firstUserId);
        assertThat(currentSpeaker.queueOrder()).isEqualTo(1);
        assertThat(currentSpeaker.status()).isEqualTo(SpeakingQueueStatus.ASSIGNED);
        assertThat(currentSpeaker.assignedAt()).isNotNull();

        StageQueueRes waitingQueue = speakingQueueService.getWaitingQueue(roomId);
        assertThat(waitingQueue.items())
                .extracting(StageQueueRes.StageQueueItemRes::userId)
                .isEqualTo(List.of(secondUserId));
    }

    @Test
    void assignNextSpeaker_throwsWhenCurrentSpeakerAlreadyExists() {
        Long roomId = createOpenRoom();
        Long firstUserId = createActiveUser();
        Long secondUserId = createActiveUser();

        speakingQueueService.requestSpeakingTurn(roomId, firstUserId);
        speakingQueueService.requestSpeakingTurn(roomId, secondUserId);
        speakingQueueService.assignNextSpeaker(roomId);

        assertThatThrownBy(() -> speakingQueueService.assignNextSpeaker(roomId))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.CURRENT_SPEAKER_ALREADY_EXISTS);
    }

    @Test
    void assignNextSpeaker_throwsWhenWaitingQueueIsEmpty() {
        Long roomId = createOpenRoom();

        assertThatThrownBy(() -> speakingQueueService.assignNextSpeaker(roomId))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.SPEAKING_QUEUE_EMPTY);
    }

    @Test
    void getCurrentSpeaker_returnsAssignedRequest() {
        Long roomId = createOpenRoom();
        Long userId = createActiveUser();

        speakingQueueService.requestSpeakingTurn(roomId, userId);
        speakingQueueService.assignNextSpeaker(roomId);

        CurrentSpeakerRes currentSpeaker = speakingQueueService.getCurrentSpeaker(roomId);

        assertThat(currentSpeaker.userId()).isEqualTo(userId);
        assertThat(currentSpeaker.status()).isEqualTo(SpeakingQueueStatus.ASSIGNED);
    }

    @Test
    void getCurrentSpeaker_throwsWhenCurrentSpeakerDoesNotExist() {
        Long roomId = createOpenRoom();

        assertThatThrownBy(() -> speakingQueueService.getCurrentSpeaker(roomId))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.CURRENT_SPEAKER_NOT_FOUND);
    }

    @Test
    void completeCurrentSpeaker_completesCurrentSpeakerAndAssignsNextWaitingRequest() {
        Long roomId = createOpenRoom();
        Long firstUserId = createActiveUser();
        Long secondUserId = createActiveUser();

        speakingQueueService.requestSpeakingTurn(roomId, firstUserId);
        speakingQueueService.requestSpeakingTurn(roomId, secondUserId);
        speakingQueueService.assignNextSpeaker(roomId);

        StageCompleteRes result = speakingQueueService.completeCurrentSpeaker(roomId, firstUserId);

        assertThat(result.completedSpeaker().userId()).isEqualTo(firstUserId);
        assertThat(result.completedSpeaker().status()).isEqualTo(SpeakingQueueStatus.COMPLETED);
        assertThat(result.nextSpeaker()).isNotNull();
        assertThat(result.nextSpeaker().userId()).isEqualTo(secondUserId);
        assertThat(result.nextSpeaker().status()).isEqualTo(SpeakingQueueStatus.ASSIGNED);

        CurrentSpeakerRes currentSpeaker = speakingQueueService.getCurrentSpeaker(roomId);
        assertThat(currentSpeaker.userId()).isEqualTo(secondUserId);

        StageQueueRes waitingQueue = speakingQueueService.getWaitingQueue(roomId);
        assertThat(waitingQueue.items()).isEmpty();
    }

    @Test
    void completeCurrentSpeaker_completesCurrentSpeakerWithoutNextWaitingRequest() {
        Long roomId = createOpenRoom();
        Long userId = createActiveUser();

        speakingQueueService.requestSpeakingTurn(roomId, userId);
        speakingQueueService.assignNextSpeaker(roomId);

        StageCompleteRes result = speakingQueueService.completeCurrentSpeaker(roomId, userId);

        assertThat(result.completedSpeaker().userId()).isEqualTo(userId);
        assertThat(result.completedSpeaker().status()).isEqualTo(SpeakingQueueStatus.COMPLETED);
        assertThat(result.nextSpeaker()).isNull();
        assertThatThrownBy(() -> speakingQueueService.getCurrentSpeaker(roomId))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.CURRENT_SPEAKER_NOT_FOUND);
    }

    @Test
    void completeCurrentSpeaker_throwsWhenCurrentSpeakerDoesNotExist() {
        Long roomId = createOpenRoom();
        Long userId = createActiveUser();

        assertThatThrownBy(() -> speakingQueueService.completeCurrentSpeaker(roomId, userId))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.CURRENT_SPEAKER_NOT_FOUND);
    }

    @Test
    void completeCurrentSpeaker_throwsWhenRequesterIsNotCurrentSpeaker() {
        Long roomId = createOpenRoom();
        Long currentSpeakerUserId = createActiveUser();
        Long otherUserId = createActiveUser();

        speakingQueueService.requestSpeakingTurn(roomId, currentSpeakerUserId);
        speakingQueueService.assignNextSpeaker(roomId);

        assertThatThrownBy(() -> speakingQueueService.completeCurrentSpeaker(roomId, otherUserId))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.FORBIDDEN);
    }

    @Test
    void expireCurrentSpeakerIfTimedOut_expiresCurrentSpeakerAndAssignsNextWaitingRequest() {
        Long roomId = createOpenRoom();
        Long firstUserId = createActiveUser();
        Long secondUserId = createActiveUser();

        speakingQueueService.requestSpeakingTurn(roomId, firstUserId);
        speakingQueueService.requestSpeakingTurn(roomId, secondUserId);
        speakingQueueService.assignNextSpeaker(roomId);

        LocalDateTime now = LocalDateTime.now().plusMinutes(10);
        StageExpireRes result = speakingQueueService.expireCurrentSpeakerIfTimedOut(
                roomId,
                now,
                Duration.ofMinutes(5)
        );

        assertThat(result.expiredSpeaker().userId()).isEqualTo(firstUserId);
        assertThat(result.expiredSpeaker().status()).isEqualTo(SpeakingQueueStatus.EXPIRED);
        assertThat(result.expiredSpeaker().endedAt()).isEqualTo(now);
        assertThat(result.nextSpeaker()).isNotNull();
        assertThat(result.nextSpeaker().userId()).isEqualTo(secondUserId);
        assertThat(result.nextSpeaker().status()).isEqualTo(SpeakingQueueStatus.ASSIGNED);
        assertThat(result.nextSpeaker().assignedAt()).isEqualTo(now);

        CurrentSpeakerRes currentSpeaker = speakingQueueService.getCurrentSpeaker(roomId);
        assertThat(currentSpeaker.userId()).isEqualTo(secondUserId);
    }

    @Test
    void expireCurrentSpeakerIfTimedOut_expiresCurrentSpeakerWithoutNextWaitingRequest() {
        Long roomId = createOpenRoom();
        Long userId = createActiveUser();

        speakingQueueService.requestSpeakingTurn(roomId, userId);
        speakingQueueService.assignNextSpeaker(roomId);

        LocalDateTime now = LocalDateTime.now().plusMinutes(10);
        StageExpireRes result = speakingQueueService.expireCurrentSpeakerIfTimedOut(
                roomId,
                now,
                Duration.ofMinutes(5)
        );

        assertThat(result.expiredSpeaker().userId()).isEqualTo(userId);
        assertThat(result.expiredSpeaker().status()).isEqualTo(SpeakingQueueStatus.EXPIRED);
        assertThat(result.nextSpeaker()).isNull();
        assertThatThrownBy(() -> speakingQueueService.getCurrentSpeaker(roomId))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.CURRENT_SPEAKER_NOT_FOUND);
    }

    @Test
    void expireCurrentSpeakerIfTimedOut_throwsWhenCurrentSpeakerIsNotTimedOut() {
        Long roomId = createOpenRoom();
        Long userId = createActiveUser();

        speakingQueueService.requestSpeakingTurn(roomId, userId);
        speakingQueueService.assignNextSpeaker(roomId);

        assertThatThrownBy(() -> speakingQueueService.expireCurrentSpeakerIfTimedOut(
                roomId,
                LocalDateTime.now(),
                Duration.ofMinutes(5)
        ))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.SPEAKING_TIME_NOT_EXPIRED);
    }

    @Test
    void expireCurrentSpeakerIfTimedOut_throwsWhenCurrentSpeakerDoesNotExist() {
        Long roomId = createOpenRoom();

        assertThatThrownBy(() -> speakingQueueService.expireCurrentSpeakerIfTimedOut(
                roomId,
                LocalDateTime.now(),
                Duration.ofMinutes(5)
        ))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.CURRENT_SPEAKER_NOT_FOUND);
    }

    @Test
    void requestSpeakingTurn_throwsWhenRoomDoesNotExist() {
        Long userId = createActiveUser();

        assertThatThrownBy(() -> speakingQueueService.requestSpeakingTurn(999_999L, userId))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ROOM_NOT_FOUND);
    }

    @Test
    void requestSpeakingTurn_throwsWhenRoomIsClosed() {
        Long roomId = createClosedRoom();
        Long userId = createActiveUser();

        assertThatThrownBy(() -> speakingQueueService.requestSpeakingTurn(roomId, userId))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ROOM_NOT_OPEN);
    }

    @Test
    void requestSpeakingTurn_throwsWhenUserDoesNotExist() {
        Long roomId = createOpenRoom();

        assertThatThrownBy(() -> speakingQueueService.requestSpeakingTurn(roomId, 999_999L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USER_NOT_FOUND);
    }

    @Test
    void requestSpeakingTurn_throwsWhenUserIsBanned() {
        Long roomId = createOpenRoom();
        Long userId = createBannedUser();

        assertThatThrownBy(() -> speakingQueueService.requestSpeakingTurn(roomId, userId))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USER_BANNED);
    }

    private Long createOpenRoom() {
        return roomRepository.save(Room.open()).getId();
    }

    private Long createClosedRoom() {
        return roomRepository.save(Room.closed()).getId();
    }

    private Long createActiveUser() {
        return userRepository.save(User.active()).getId();
    }

    private Long createBannedUser() {
        return userRepository.save(User.banned()).getId();
    }
}
