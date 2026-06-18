package com.sisibibi.api.domain.speech.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.sisibibi.api.domain.speech.config.SpeakingQueueProperties;
import com.sisibibi.api.domain.speech.dto.response.StageCurrentSpeakerRes;
import com.sisibibi.api.domain.speech.dto.response.StageQueueRes;
import com.sisibibi.api.domain.speech.dto.response.StageRequestRes;
import com.sisibibi.api.domain.speech.dto.response.StageRequestStatusRes;
import com.sisibibi.api.domain.speech.entity.SpeakingQueue;
import com.sisibibi.api.domain.speech.entity.SpeakingQueueStatus;
import com.sisibibi.api.domain.speech.repository.RedisSpeakingQueueRepository;
import com.sisibibi.api.domain.speech.repository.projection.CurrentSpeakerProjection;
import com.sisibibi.api.global.exception.CustomException;
import com.sisibibi.api.global.exception.ErrorCode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SpeakingQueueServiceTest {

    @Mock
    private RedisSpeakingQueueRepository redisSpeakingQueueRepository;

    @Mock
    private SpeakingQueuePersistenceService speakingQueuePersistenceService;

    @Mock
    private SpeakingQueueProperties speakingQueueProperties;

    @InjectMocks
    private SpeakingQueueService speakingQueueService;

    @Test
    void requestSpeakingTurn_persistsWaitingRequestWithoutSynchronousAssignment() {
        SpeakingQueue saved = persistedWaitingRequest(1L, 7L, 15);
        given(speakingQueuePersistenceService.createWaitingRequest(1L, 7L))
                .willReturn(saved);

        StageRequestRes response = speakingQueueService.requestSpeakingTurn(1L, 7L);

        assertThat(response.roomId()).isEqualTo(1L);
        assertThat(response.userId()).isEqualTo(7L);
        assertThat(response.status()).isEqualTo(SpeakingQueueStatus.WAITING);
        assertThat(response.queueOrder()).isEqualTo(15);
        verify(redisSpeakingQueueRepository).upsert(1L, 7L, 15);
        verify(redisSpeakingQueueRepository, never()).assign(1L, 7L);
        verify(speakingQueuePersistenceService, never()).assignNextSpeaker(
                eq(1L),
                any(LocalDateTime.class),
                any(LocalDateTime.class)
        );
    }

    @Test
    void requestSpeakingTurn_doesNotWriteRedisWhenRdbPersistenceFails() {
        given(speakingQueuePersistenceService.createWaitingRequest(1L, 7L))
                .willThrow(new IllegalStateException("database unavailable"));

        assertThatThrownBy(() -> speakingQueueService.requestSpeakingTurn(1L, 7L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("database unavailable");

        verify(redisSpeakingQueueRepository, never())
                .upsert(org.mockito.ArgumentMatchers.anyLong(),
                        org.mockito.ArgumentMatchers.anyLong(),
                        org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    void requestSpeakingTurn_keepsDurableRequestWhenRedisSynchronizationFails() {
        SpeakingQueue saved = persistedWaitingRequest(1L, 7L, 15);
        given(speakingQueuePersistenceService.createWaitingRequest(1L, 7L))
                .willReturn(saved);
        doThrow(new IllegalStateException("redis unavailable"))
                .when(redisSpeakingQueueRepository)
                .upsert(1L, 7L, 15);

        StageRequestRes response = speakingQueueService.requestSpeakingTurn(1L, 7L);

        assertThat(response.queueOrder()).isEqualTo(15);
        assertThat(response.status()).isEqualTo(SpeakingQueueStatus.WAITING);
        verify(speakingQueuePersistenceService).createWaitingRequest(1L, 7L);
        verify(speakingQueuePersistenceService, never()).assignNextSpeaker(
                eq(1L),
                any(LocalDateTime.class),
                any(LocalDateTime.class)
        );
    }

    @Test
    void assignNextSpeaker_appliesConfiguredTurnDuration() {
        given(speakingQueueProperties.getTurnDuration())
                .willReturn(Duration.ofSeconds(90));
        given(speakingQueuePersistenceService.assignNextSpeaker(
                eq(1L),
                any(LocalDateTime.class),
                any(LocalDateTime.class)
        )).willReturn(Optional.empty());

        speakingQueueService.assignNextSpeaker(1L);

        ArgumentCaptor<LocalDateTime> assignedAtCaptor =
                ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> expiresAtCaptor =
                ArgumentCaptor.forClass(LocalDateTime.class);
        verify(speakingQueuePersistenceService).assignNextSpeaker(
                eq(1L),
                assignedAtCaptor.capture(),
                expiresAtCaptor.capture()
        );
        assertThat(Duration.between(
                assignedAtCaptor.getValue(),
                expiresAtCaptor.getValue()
        )).isEqualTo(Duration.ofSeconds(90));
    }

    @Test
    void cancelSpeakingRequest_cancelsDurableRequestAndRemovesRedisProjection() {
        SpeakingQueue canceled = persistedWaitingRequest(1L, 7L, 15);
        canceled.cancel(LocalDateTime.of(2026, 6, 12, 11, 35));
        given(speakingQueuePersistenceService.cancelWaitingRequest(1L, 7L))
                .willReturn(canceled);

        speakingQueueService.cancelSpeakingRequest(1L, 7L);

        verify(speakingQueuePersistenceService).cancelWaitingRequest(1L, 7L);
        verify(redisSpeakingQueueRepository).remove(1L, 7L);
    }

    @Test
    void cancelSpeakingRequest_keepsCanceledRdbStateWhenRedisRemovalFails() {
        SpeakingQueue canceled = persistedWaitingRequest(1L, 7L, 15);
        canceled.cancel(LocalDateTime.of(2026, 6, 12, 11, 35));
        given(speakingQueuePersistenceService.cancelWaitingRequest(1L, 7L))
                .willReturn(canceled);
        doThrow(new IllegalStateException("redis unavailable"))
                .when(redisSpeakingQueueRepository)
                .remove(1L, 7L);

        speakingQueueService.cancelSpeakingRequest(1L, 7L);

        verify(speakingQueuePersistenceService).cancelWaitingRequest(1L, 7L);
        verify(redisSpeakingQueueRepository).remove(1L, 7L);
    }

    @Test
    void getMySpeakingRequestStatus_returnsEmptyResponseWhenActiveRequestDoesNotExist() {
        given(speakingQueuePersistenceService.findMyActiveRequest(1L, 7L))
                .willReturn(Optional.empty());

        StageRequestStatusRes response =
                speakingQueueService.getMySpeakingRequestStatus(1L, 7L);

        assertThat(response.hasRequest()).isFalse();
        assertThat(response.status()).isNull();
        assertThat(response.cancelable()).isFalse();
    }

    @Test
    void getMySpeakingRequestStatus_returnsWaitingRequestStatus() {
        SpeakingQueue waiting = persistedWaitingRequest(1L, 7L, 15);
        given(speakingQueuePersistenceService.findMyActiveRequest(1L, 7L))
                .willReturn(Optional.of(waiting));
        given(redisSpeakingQueueRepository.rank(1L, 7L))
                .willReturn(Optional.of(3));

        StageRequestStatusRes response =
                speakingQueueService.getMySpeakingRequestStatus(1L, 7L);

        assertThat(response.hasRequest()).isTrue();
        assertThat(response.status()).isEqualTo(SpeakingQueueStatus.WAITING);
        assertThat(response.roomId()).isEqualTo(1L);
        assertThat(response.userId()).isEqualTo(7L);
        assertThat(response.queueOrder()).isEqualTo(15);
        assertThat(response.currentRank()).isEqualTo(3);
        assertThat(response.cancelable()).isTrue();
        assertThat(response.requestedAt())
                .isEqualTo(LocalDateTime.of(2026, 6, 12, 11, 30));
        assertThat(response.assignedAt()).isNull();
        assertThat(response.expiresAt()).isNull();
    }

    @Test
    void getMySpeakingRequestStatus_returnsNullRankWhenWaitingRequestIsMissingInRedis() {
        SpeakingQueue waiting = persistedWaitingRequest(1L, 7L, 15);
        given(speakingQueuePersistenceService.findMyActiveRequest(1L, 7L))
                .willReturn(Optional.of(waiting));
        given(redisSpeakingQueueRepository.rank(1L, 7L))
                .willReturn(Optional.empty());

        StageRequestStatusRes response =
                speakingQueueService.getMySpeakingRequestStatus(1L, 7L);

        assertThat(response.hasRequest()).isTrue();
        assertThat(response.status()).isEqualTo(SpeakingQueueStatus.WAITING);
        assertThat(response.currentRank()).isNull();
    }

    @Test
    void getMySpeakingRequestStatus_returnsAssignedRequestStatus() {
        SpeakingQueue assigned = assignedRequest(1L, 7L, 15);
        given(speakingQueuePersistenceService.findMyActiveRequest(1L, 7L))
                .willReturn(Optional.of(assigned));

        StageRequestStatusRes response =
                speakingQueueService.getMySpeakingRequestStatus(1L, 7L);

        assertThat(response.hasRequest()).isTrue();
        assertThat(response.status()).isEqualTo(SpeakingQueueStatus.ASSIGNED);
        assertThat(response.queueOrder()).isEqualTo(15);
        assertThat(response.currentRank()).isNull();
        assertThat(response.cancelable()).isFalse();
        assertThat(response.assignedAt())
                .isEqualTo(LocalDateTime.of(2026, 6, 12, 11, 31));
        assertThat(response.expiresAt())
                .isEqualTo(LocalDateTime.of(2026, 6, 12, 11, 33));
        verify(redisSpeakingQueueRepository, never()).rank(1L, 7L);
    }

    @Test
    void completeSpeakingTurn_completesDurableRequestAndRemovesCurrentSpeaker() {
        SpeakingQueue completed = completedRequest(1L, 7L, 15);
        given(speakingQueuePersistenceService.completeCurrentSpeaker(1L, 7L))
                .willReturn(completed);

        speakingQueueService.completeSpeakingTurn(1L, 7L);

        verify(speakingQueuePersistenceService).completeCurrentSpeaker(1L, 7L);
        verify(redisSpeakingQueueRepository).removeCurrentSpeaker(1L, 7L);
    }

    @Test
    void completeSpeakingTurn_keepsCompletedRdbStateWhenRedisRemovalFails() {
        SpeakingQueue completed = completedRequest(1L, 7L, 15);
        given(speakingQueuePersistenceService.completeCurrentSpeaker(1L, 7L))
                .willReturn(completed);
        doThrow(new IllegalStateException("redis unavailable"))
                .when(redisSpeakingQueueRepository)
                .removeCurrentSpeaker(1L, 7L);

        speakingQueueService.completeSpeakingTurn(1L, 7L);

        verify(speakingQueuePersistenceService).completeCurrentSpeaker(1L, 7L);
        verify(redisSpeakingQueueRepository).removeCurrentSpeaker(1L, 7L);
    }

    @Test
    void expireCurrentSpeaker_removesExpiredSpeakerFromRedis() {
        SpeakingQueue completed = completedRequest(1L, 7L, 15);
        LocalDateTime now = LocalDateTime.of(2026, 6, 12, 11, 34);
        given(speakingQueuePersistenceService.expireCurrentSpeaker(1L, now))
                .willReturn(Optional.of(completed));

        Optional<SpeakingQueue> expired =
                speakingQueueService.expireCurrentSpeaker(1L, now);

        assertThat(expired).contains(completed);
        verify(redisSpeakingQueueRepository).removeCurrentSpeaker(1L, 7L);
    }

    @Test
    void expireCurrentSpeaker_doesNotTouchRedisWhenSpeakerIsNotExpired() {
        LocalDateTime now = LocalDateTime.of(2026, 6, 12, 11, 32);
        given(speakingQueuePersistenceService.expireCurrentSpeaker(1L, now))
                .willReturn(Optional.empty());

        Optional<SpeakingQueue> expired =
                speakingQueueService.expireCurrentSpeaker(1L, now);

        assertThat(expired).isEmpty();
        verify(redisSpeakingQueueRepository, never())
                .removeCurrentSpeaker(
                        org.mockito.ArgumentMatchers.anyLong(),
                        org.mockito.ArgumentMatchers.anyLong()
                );
    }

    @Test
    void getCurrentSpeaker_returnsEmptyResponseWhenCurrentSpeakerDoesNotExist() {
        given(speakingQueuePersistenceService.findCurrentSpeaker(1L))
                .willReturn(Optional.empty());

        StageCurrentSpeakerRes response =
                speakingQueueService.getCurrentSpeaker(1L);

        assertThat(response.hasCurrentSpeaker()).isFalse();
        assertThat(response.currentSpeaker()).isNull();
    }

    @Test
    void getCurrentSpeaker_returnsCurrentSpeakerWithNickname() {
        CurrentSpeakerProjection currentSpeaker = currentSpeakerProjection(
                7L,
                "logic_hunter",
                15,
                LocalDateTime.of(2026, 6, 12, 11, 31),
                LocalDateTime.of(2026, 6, 12, 11, 33)
        );
        given(speakingQueuePersistenceService.findCurrentSpeaker(1L))
                .willReturn(Optional.of(currentSpeaker));

        StageCurrentSpeakerRes response =
                speakingQueueService.getCurrentSpeaker(1L);

        assertThat(response.hasCurrentSpeaker()).isTrue();
        assertThat(response.currentSpeaker().userId()).isEqualTo(7L);
        assertThat(response.currentSpeaker().nickname()).isEqualTo("logic_hunter");
        assertThat(response.currentSpeaker().queueOrder()).isEqualTo(15);
        assertThat(response.currentSpeaker().assignedAt())
                .isEqualTo(LocalDateTime.of(2026, 6, 12, 11, 31));
        assertThat(response.currentSpeaker().expiresAt())
                .isEqualTo(LocalDateTime.of(2026, 6, 12, 11, 33));
    }

    @Test
    void getQueueSummary_returnsFirstFiveWaitingSpeakers() {
        List<Long> userIds = List.of(10L, 20L);
        givenQueueProperties(5, 20, 100);
        given(redisSpeakingQueueRepository.count(1L)).willReturn(8L);
        given(redisSpeakingQueueRepository.findWaitingUserIds(1L, 0, 4))
                .willReturn(userIds);
        given(speakingQueuePersistenceService.findNicknamesByUserIds(userIds))
                .willReturn(Map.of(
                        10L, "logic_hunter",
                        20L, "dream_catcher"
                ));

        StageQueueRes response = speakingQueueService.getQueueSummary(1L);

        assertThat(response.totalWaitingCount()).isEqualTo(8L);
        assertThat(response.offset()).isZero();
        assertThat(response.size()).isEqualTo(5);
        assertThat(response.hasNext()).isTrue();
        assertThat(response.items()).hasSize(2);
        assertThat(response.items().get(0).rank()).isEqualTo(1);
        assertThat(response.items().get(0).userId()).isEqualTo(10L);
        assertThat(response.items().get(0).nickname()).isEqualTo("logic_hunter");
        assertThat(response.items().get(1).rank()).isEqualTo(2);
        assertThat(response.items().get(1).userId()).isEqualTo(20L);
        assertThat(response.items().get(1).nickname()).isEqualTo("dream_catcher");
    }

    @Test
    void getWaitingQueue_returnsPagedWaitingSpeakersWithRankOffset() {
        List<Long> userIds = List.of(30L, 40L);
        givenQueueProperties(5, 20, 100);
        given(redisSpeakingQueueRepository.count(1L)).willReturn(4L);
        given(redisSpeakingQueueRepository.findWaitingUserIds(1L, 2, 3))
                .willReturn(userIds);
        given(speakingQueuePersistenceService.findNicknamesByUserIds(userIds))
                .willReturn(Map.of(
                        30L, "neon_wave",
                        40L, "open_mind"
                ));

        StageQueueRes response = speakingQueueService.getWaitingQueue(1L, 2, 2);

        assertThat(response.totalWaitingCount()).isEqualTo(4L);
        assertThat(response.offset()).isEqualTo(2);
        assertThat(response.size()).isEqualTo(2);
        assertThat(response.hasNext()).isFalse();
        assertThat(response.items()).hasSize(2);
        assertThat(response.items().get(0).rank()).isEqualTo(3);
        assertThat(response.items().get(0).nickname()).isEqualTo("neon_wave");
        assertThat(response.items().get(1).rank()).isEqualTo(4);
        assertThat(response.items().get(1).nickname()).isEqualTo("open_mind");
    }

    @Test
    void getWaitingQueue_returnsEmptyItemsWhenQueueIsEmpty() {
        givenQueueProperties(5, 20, 100);
        given(redisSpeakingQueueRepository.count(1L)).willReturn(0L);
        given(redisSpeakingQueueRepository.findWaitingUserIds(1L, 0, 19))
                .willReturn(List.of());
        given(speakingQueuePersistenceService.findNicknamesByUserIds(List.of()))
                .willReturn(Map.of());

        StageQueueRes response = speakingQueueService.getWaitingQueue(1L, 0, 20);

        assertThat(response.totalWaitingCount()).isZero();
        assertThat(response.hasNext()).isFalse();
        assertThat(response.items()).isEmpty();
    }

    @Test
    void getWaitingQueue_usesConfiguredDefaultPageSizeWhenSizeIsMissing() {
        givenQueueProperties(5, 30, 100);
        given(redisSpeakingQueueRepository.count(1L)).willReturn(0L);
        given(redisSpeakingQueueRepository.findWaitingUserIds(1L, 0, 29))
                .willReturn(List.of());
        given(speakingQueuePersistenceService.findNicknamesByUserIds(List.of()))
                .willReturn(Map.of());

        StageQueueRes response = speakingQueueService.getWaitingQueue(1L, null, null);

        assertThat(response.offset()).isZero();
        assertThat(response.size()).isEqualTo(30);
        assertThat(response.items()).isEmpty();
    }

    @Test
    void getWaitingQueue_rejectsSizeGreaterThanConfiguredMaxPageSize() {
        givenQueueProperties(5, 20, 50);

        assertThatThrownBy(() -> speakingQueueService.getWaitingQueue(1L, 0, 51))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
    }

    private void givenQueueProperties(
            int summarySize,
            int defaultPageSize,
            int maxPageSize
    ) {
        SpeakingQueueProperties.Queue queue = new SpeakingQueueProperties.Queue();
        queue.setSummarySize(summarySize);
        queue.setDefaultPageSize(defaultPageSize);
        queue.setMaxPageSize(maxPageSize);
        given(speakingQueueProperties.getQueue()).willReturn(queue);
    }

    private CurrentSpeakerProjection currentSpeakerProjection(
            Long userId,
            String nickname,
            Integer queueOrder,
            LocalDateTime assignedAt,
            LocalDateTime expiresAt
    ) {
        return new CurrentSpeakerProjection() {
            @Override
            public Long getUserId() {
                return userId;
            }

            @Override
            public String getNickname() {
                return nickname;
            }

            @Override
            public Integer getQueueOrder() {
                return queueOrder;
            }

            @Override
            public LocalDateTime getAssignedAt() {
                return assignedAt;
            }

            @Override
            public LocalDateTime getExpiresAt() {
                return expiresAt;
            }
        };
    }

    private SpeakingQueue persistedWaitingRequest(Long roomId, Long userId, int queueOrder) {
        return SpeakingQueue.waiting(
                roomId,
                userId,
                queueOrder,
                LocalDateTime.of(2026, 6, 12, 11, 30)
        );
    }

    private SpeakingQueue completedRequest(Long roomId, Long userId, int queueOrder) {
        SpeakingQueue speakingQueue = assignedRequest(roomId, userId, queueOrder);
        speakingQueue.complete();
        return speakingQueue;
    }

    private SpeakingQueue assignedRequest(Long roomId, Long userId, int queueOrder) {
        SpeakingQueue speakingQueue =
                persistedWaitingRequest(roomId, userId, queueOrder);
        speakingQueue.assign(
                LocalDateTime.of(2026, 6, 12, 11, 31),
                LocalDateTime.of(2026, 6, 12, 11, 33)
        );
        return speakingQueue;
    }

}
