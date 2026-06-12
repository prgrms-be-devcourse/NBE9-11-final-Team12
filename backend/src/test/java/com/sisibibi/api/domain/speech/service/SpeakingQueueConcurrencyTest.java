package com.sisibibi.api.domain.speech.service;

import com.sisibibi.api.domain.speech.dto.response.CurrentSpeakerRes;
import com.sisibibi.api.domain.speech.dto.response.StageQueueRes;
import com.sisibibi.api.domain.speech.dto.response.StageRequestRes;
import com.sisibibi.api.domain.speech.entity.SpeakingQueueStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class SpeakingQueueConcurrencyTest {

    @Autowired
    private SpeakingQueueService speakingQueueService;

    @Test
    void concurrentSpeakingRequests_documentsCurrentQueueOrderConflictLimitation()
            throws Exception {
        Long roomId = 100L;
        int requestCount = 20;

        List<ConcurrentResult<StageRequestRes>> results = runConcurrently(
                requestCount,
                index -> speakingQueueService.requestSpeakingTurn(roomId, 1000L + index)
        );

        List<StageRequestRes> successes = successes(results);
        List<Throwable> failures = failures(results);
        StageQueueRes waitingQueue = speakingQueueService.getWaitingQueue(roomId);

        assertThat(successes).isNotEmpty();
        assertThat(failures)
                .as("현재 max(queueOrder) + 1 방식은 동시 신청 시 순번 충돌 실패가 발생할 수 있다.")
                .isNotEmpty();
        assertThat(waitingQueue.items()).hasSameSizeAs(successes);
        assertThat(waitingQueue.items())
                .extracting(StageQueueRes.StageQueueItemRes::queueOrder)
                .doesNotHaveDuplicates();
    }

    @Test
    void concurrentAssignNextSpeaker_documentsCurrentDuplicateAssignmentResponseLimitation()
            throws Exception {
        Long roomId = 101L;
        int requestCount = 10;

        speakingQueueService.requestSpeakingTurn(roomId, 2000L);
        speakingQueueService.requestSpeakingTurn(roomId, 2001L);

        List<ConcurrentResult<CurrentSpeakerRes>> results = runConcurrently(
                requestCount,
                ignored -> speakingQueueService.assignNextSpeaker(roomId)
        );

        List<CurrentSpeakerRes> successes = successes(results);
        StageQueueRes waitingQueue = speakingQueueService.getWaitingQueue(roomId);
        CurrentSpeakerRes currentSpeaker = speakingQueueService.getCurrentSpeaker(roomId);

        assertThat(successes)
                .as("현재 배정 로직은 락이 없어 여러 호출자가 같은 발언자 배정을 성공으로 받을 수 있다.")
                .hasSizeGreaterThan(1);
        assertThat(successes)
                .extracting(CurrentSpeakerRes::id)
                .containsOnly(currentSpeaker.id());
        assertThat(currentSpeaker.status()).isEqualTo(SpeakingQueueStatus.ASSIGNED);
        assertThat(waitingQueue.items())
                .extracting(StageQueueRes.StageQueueItemRes::userId)
                .containsExactly(2001L);
    }

    private <T> List<ConcurrentResult<T>> runConcurrently(
            int taskCount,
            ConcurrentTask<T> concurrentTask
    ) throws Exception {
        ExecutorService executorService = Executors.newFixedThreadPool(taskCount);
        CountDownLatch ready = new CountDownLatch(taskCount);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<ConcurrentResult<T>>> futures = new ArrayList<>();

        try {
            for (int index = 0; index < taskCount; index++) {
                int taskIndex = index;
                futures.add(executorService.submit(toCallable(
                        taskIndex,
                        ready,
                        start,
                        concurrentTask
                )));
            }

            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            List<ConcurrentResult<T>> results = new ArrayList<>();

            for (Future<ConcurrentResult<T>> future : futures) {
                results.add(future.get(5, TimeUnit.SECONDS));
            }

            return results;
        } finally {
            executorService.shutdownNow();
        }
    }

    private <T> Callable<ConcurrentResult<T>> toCallable(
            int index,
            CountDownLatch ready,
            CountDownLatch start,
            ConcurrentTask<T> concurrentTask
    ) {
        return () -> {
            ready.countDown();
            start.await();

            try {
                return ConcurrentResult.success(concurrentTask.execute(index));
            } catch (Throwable throwable) {
                return ConcurrentResult.failure(throwable);
            }
        };
    }

    private <T> List<T> successes(List<ConcurrentResult<T>> results) {
        return results.stream()
                .filter(ConcurrentResult::isSuccess)
                .map(ConcurrentResult::value)
                .toList();
    }

    private <T> List<Throwable> failures(List<ConcurrentResult<T>> results) {
        return results.stream()
                .filter(result -> !result.isSuccess())
                .map(ConcurrentResult::throwable)
                .toList();
    }

    @FunctionalInterface
    private interface ConcurrentTask<T> {

        T execute(int index);
    }

    private record ConcurrentResult<T>(
            T value,
            Throwable throwable
    ) {

        static <T> ConcurrentResult<T> success(T value) {
            return new ConcurrentResult<>(value, null);
        }

        static <T> ConcurrentResult<T> failure(Throwable throwable) {
            return new ConcurrentResult<>(null, throwable);
        }

        boolean isSuccess() {
            return throwable == null;
        }
    }
}
