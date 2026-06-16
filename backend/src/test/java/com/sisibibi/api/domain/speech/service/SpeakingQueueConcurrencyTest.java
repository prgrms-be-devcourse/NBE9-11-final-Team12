package com.sisibibi.api.domain.speech.service;

import com.sisibibi.api.domain.room.entity.Room;
import com.sisibibi.api.domain.room.repository.RoomRepository;
import com.sisibibi.api.domain.speech.dto.response.CurrentSpeakerRes;
import com.sisibibi.api.domain.speech.dto.response.StageCompleteRes;
import com.sisibibi.api.domain.speech.dto.response.StageExpireRes;
import com.sisibibi.api.domain.speech.dto.response.StageQueueRes;
import com.sisibibi.api.domain.speech.dto.response.StageRequestRes;
import com.sisibibi.api.domain.speech.entity.SpeakingQueueStatus;
import com.sisibibi.api.domain.user.entity.User;
import com.sisibibi.api.domain.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.io.UncheckedIOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.LocalDateTime;
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

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void concurrentSpeakingRequests_assignsUniqueQueueOrdersWithRoomLock()
            throws Exception {
        Long roomId = createOpenRoom();
        int requestCount = 20;
        List<Long> userIds = createActiveUsers(requestCount);

        ConcurrentRun<StageRequestRes> run = runConcurrently(
                requestCount,
                index -> speakingQueueService.requestSpeakingTurn(roomId, userIds.get(index))
        );
        List<ConcurrentResult<StageRequestRes>> results = run.results();

        List<StageRequestRes> successes = successes(results);
        List<Throwable> failures = failures(results);
        StageQueueRes waitingQueue = speakingQueueService.getWaitingQueue(roomId);
        CurrentSpeakerRes currentSpeaker = speakingQueueService.getCurrentSpeaker(roomId);

        logConcurrencySummary(
                "동시 발언권 신청",
                requestCount,
                successes.size(),
                failures,
                run,
                "현재 발언자 userId=" + currentSpeaker.userId()
                        + ", 저장된 대기열 크기=" + waitingQueue.items().size()
        );

        assertThat(successes).hasSize(requestCount);
        assertThat(failures).isEmpty();
        assertThat(currentSpeaker.status()).isEqualTo(SpeakingQueueStatus.ASSIGNED);
        assertThat(waitingQueue.items()).hasSize(requestCount - 1);
        assertThat(successes)
                .extracting(StageRequestRes::queueOrder)
                .containsExactlyInAnyOrderElementsOf(rangeClosed(1, requestCount));
        assertThat(waitingQueue.items())
                .extracting(StageQueueRes.StageQueueItemRes::queueOrder)
                .containsExactlyElementsOf(rangeClosed(2, requestCount));
    }

    @Test
    void duplicateAssignNextSpeakerRequest_failsWhenCurrentSpeakerAlreadyExists()
            throws Exception {
        Long roomId = createOpenRoom();
        int requestCount = 10;
        Long firstUserId = createActiveUser();
        Long secondUserId = createActiveUser();

        speakingQueueService.requestSpeakingTurn(roomId, firstUserId);
        speakingQueueService.requestSpeakingTurn(roomId, secondUserId);

        ConcurrentRun<CurrentSpeakerRes> run = runConcurrently(
                requestCount,
                ignored -> speakingQueueService.assignNextSpeaker(roomId)
        );
        List<ConcurrentResult<CurrentSpeakerRes>> results = run.results();

        List<CurrentSpeakerRes> successes = successes(results);
        List<Throwable> failures = failures(results);
        StageQueueRes waitingQueue = speakingQueueService.getWaitingQueue(roomId);
        CurrentSpeakerRes currentSpeaker = speakingQueueService.getCurrentSpeaker(roomId);

        logConcurrencySummary(
                "중복 현재 발언자 배정 요청 방어",
                requestCount,
                successes.size(),
                failures,
                run,
                "현재 발언자 queueId=" + currentSpeaker.id()
                        + ", 남은 대기열 크기=" + waitingQueue.items().size()
        );

        assertThat(successes)
                .as("현재 발언자가 이미 있으면 중복 배정 시도는 모두 실패해야 한다.")
                .isEmpty();
        assertThat(failures).hasSize(requestCount);
        assertThat(failures)
                .extracting(throwable -> throwable.getClass().getSimpleName())
                .containsOnly("CustomException");
        assertThat(currentSpeaker.status()).isEqualTo(SpeakingQueueStatus.ASSIGNED);
        assertThat(waitingQueue.items())
                .extracting(StageQueueRes.StageQueueItemRes::userId)
                .containsExactly(secondUserId);
    }

    @Test
    void duplicateCompleteCurrentSpeakerRequest_allowsOnlyOneSuccessfulCompletionWithRoomLock()
            throws Exception {
        Long roomId = createOpenRoom();
        int requestCount = 10;
        Long currentSpeakerUserId = createActiveUser();
        Long nextUserId = createActiveUser();

        speakingQueueService.requestSpeakingTurn(roomId, currentSpeakerUserId);
        speakingQueueService.requestSpeakingTurn(roomId, nextUserId);

        ConcurrentRun<StageCompleteRes> run = runConcurrently(
                requestCount,
                ignored -> speakingQueueService.completeCurrentSpeaker(roomId, currentSpeakerUserId)
        );
        List<ConcurrentResult<StageCompleteRes>> results = run.results();

        List<StageCompleteRes> successes = successes(results);
        List<Throwable> failures = failures(results);
        StageQueueRes waitingQueue = speakingQueueService.getWaitingQueue(roomId);
        CurrentSpeakerRes currentSpeaker = speakingQueueService.getCurrentSpeaker(roomId);

        logConcurrencySummary(
                "중복 발언 종료 요청 방어",
                requestCount,
                successes.size(),
                failures,
                run,
                "현재 발언자 userId=" + currentSpeaker.userId()
                        + ", 남은 대기열 크기=" + waitingQueue.items().size()
        );

        assertThat(successes)
                .as("방 단위 락이 있으면 같은 현재 발언자 종료 요청은 하나만 성공해야 한다.")
                .hasSize(1);
        assertThat(successes.get(0).completedSpeaker().userId())
                .isEqualTo(currentSpeakerUserId);
        assertThat(successes.get(0).completedSpeaker().status())
                .isEqualTo(SpeakingQueueStatus.COMPLETED);
        assertThat(successes.get(0).nextSpeaker()).isNotNull();
        assertThat(successes.get(0).nextSpeaker().userId()).isEqualTo(nextUserId);
        assertThat(failures).hasSize(requestCount - 1);
        assertThat(failures)
                .extracting(throwable -> throwable.getClass().getSimpleName())
                .containsOnly("CustomException");
        assertThat(currentSpeaker.userId()).isEqualTo(nextUserId);
        assertThat(currentSpeaker.status()).isEqualTo(SpeakingQueueStatus.ASSIGNED);
        assertThat(waitingQueue.items()).isEmpty();
    }

    @Test
    void duplicateSchedulerExpiration_allowsOnlyOneSuccessfulExpirationWithRoomLock()
            throws Exception {
        Long roomId = createOpenRoom();
        int requestCount = 10;
        Long currentSpeakerUserId = createActiveUser();
        Long nextUserId = createActiveUser();
        LocalDateTime now = LocalDateTime.now().plusMinutes(10);

        speakingQueueService.requestSpeakingTurn(roomId, currentSpeakerUserId);
        speakingQueueService.requestSpeakingTurn(roomId, nextUserId);

        ConcurrentRun<StageExpireRes> run = runConcurrently(
                requestCount,
                ignored -> speakingQueueService.expireCurrentSpeakerIfTimedOut(
                        roomId,
                        now,
                        Duration.ofMinutes(5)
                )
        );
        List<ConcurrentResult<StageExpireRes>> results = run.results();

        List<StageExpireRes> successes = successes(results);
        List<Throwable> failures = failures(results);
        StageQueueRes waitingQueue = speakingQueueService.getWaitingQueue(roomId);
        CurrentSpeakerRes currentSpeaker = speakingQueueService.getCurrentSpeaker(roomId);

        logConcurrencySummary(
                "중복 Scheduler 자동 만료 실행 방어",
                requestCount,
                successes.size(),
                failures,
                run,
                "현재 발언자 userId=" + currentSpeaker.userId()
                        + ", 남은 대기열 크기=" + waitingQueue.items().size()
        );

        assertThat(successes)
                .as("방 단위 락이 있으면 같은 현재 발언자 자동 만료 요청은 하나만 성공해야 한다.")
                .hasSize(1);
        assertThat(successes.get(0).expiredSpeaker().userId())
                .isEqualTo(currentSpeakerUserId);
        assertThat(successes.get(0).expiredSpeaker().status())
                .isEqualTo(SpeakingQueueStatus.EXPIRED);
        assertThat(successes.get(0).nextSpeaker()).isNotNull();
        assertThat(successes.get(0).nextSpeaker().userId()).isEqualTo(nextUserId);
        assertThat(failures).hasSize(requestCount - 1);
        assertThat(failures)
                .extracting(throwable -> throwable.getClass().getSimpleName())
                .containsOnly("CustomException");
        assertThat(currentSpeaker.userId()).isEqualTo(nextUserId);
        assertThat(currentSpeaker.status()).isEqualTo(SpeakingQueueStatus.ASSIGNED);
        assertThat(waitingQueue.items()).isEmpty();
    }

    @Test
    void completeAndExpireRace_allowsOnlyOneTerminalTransitionWithRoomLock()
            throws Exception {
        Long roomId = createOpenRoom();
        Long currentSpeakerUserId = createActiveUser();
        Long nextUserId = createActiveUser();

        speakingQueueService.requestSpeakingTurn(roomId, currentSpeakerUserId);
        speakingQueueService.requestSpeakingTurn(roomId, nextUserId);
        StageRequestRes currentSpeakerBeforeRace = speakingQueueService.getMyRequest(
                roomId,
                currentSpeakerUserId
        );
        Duration speakingTimeLimit = Duration.ofMinutes(3);
        LocalDateTime schedulerNow =
                currentSpeakerBeforeRace.assignedAt().plus(speakingTimeLimit).plusNanos(1);

        ConcurrentRun<Object> run = runConcurrently(
                2,
                index -> {
                    if (index == 0) {
                        return speakingQueueService.completeCurrentSpeaker(
                                roomId,
                                currentSpeakerUserId
                        );
                    }

                    return speakingQueueService.expireCurrentSpeakerIfTimedOut(
                            roomId,
                            schedulerNow,
                            speakingTimeLimit
                    );
                }
        );
        List<ConcurrentResult<Object>> results = run.results();

        List<Object> successes = successes(results);
        List<Throwable> failures = failures(results);
        StageQueueRes waitingQueue = speakingQueueService.getWaitingQueue(roomId);
        CurrentSpeakerRes currentSpeaker = speakingQueueService.getCurrentSpeaker(roomId);
        StageRequestRes terminalRequest = speakingQueueService.getMyRequest(
                roomId,
                currentSpeakerUserId
        );
        StageRequestRes nextSpeakerRequest = speakingQueueService.getMyRequest(roomId, nextUserId);

        logConcurrencySummary(
                "발언 종료와 자동 만료 경합",
                2,
                successes.size(),
                failures,
                run,
                "terminalStatus=" + terminalRequest.status()
                        + ", 현재 발언자 userId=" + currentSpeaker.userId()
        );

        assertThat(successes)
                .as("사용자 종료와 Scheduler 만료가 충돌해도 terminal 상태 전이는 하나만 성공해야 한다.")
                .hasSize(1);
        assertThat(successes.get(0))
                .isInstanceOfAny(StageCompleteRes.class, StageExpireRes.class);
        assertThat(failures).hasSize(1);
        assertThat(failures)
                .extracting(throwable -> throwable.getClass().getSimpleName())
                .containsOnly("CustomException");
        assertThat(terminalRequest.status())
                .isIn(SpeakingQueueStatus.COMPLETED, SpeakingQueueStatus.EXPIRED);
        assertThat(currentSpeaker.userId()).isEqualTo(nextUserId);
        assertThat(currentSpeaker.status()).isEqualTo(SpeakingQueueStatus.ASSIGNED);
        assertThat(nextSpeakerRequest.status()).isEqualTo(SpeakingQueueStatus.ASSIGNED);
        assertThat(waitingQueue.items()).isEmpty();
    }

    private <T> ConcurrentRun<T> runConcurrently(
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
            long startedAt = System.nanoTime();
            start.countDown();

            List<ConcurrentResult<T>> results = new ArrayList<>();

            for (Future<ConcurrentResult<T>> future : futures) {
                results.add(future.get(5, TimeUnit.SECONDS));
            }

            return new ConcurrentRun<>(results, System.nanoTime() - startedAt);
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
            long startedAt = System.nanoTime();

            try {
                T result = concurrentTask.execute(index);
                return ConcurrentResult.success(result, System.nanoTime() - startedAt);
            } catch (Throwable throwable) {
                return ConcurrentResult.failure(throwable, System.nanoTime() - startedAt);
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

    private void logConcurrencySummary(
            String scenario,
            int requestCount,
            int successCount,
            List<Throwable> failures,
            ConcurrentRun<?> run,
            String extra
    ) {
        String summary = String.format(
                "[동시성 테스트 결과] scenario=%s, \n 요청 수=%d, 성공 수=%d, 실패 수=%d, 실패 타입=%s, 실행시간=%dms, 합계=%dms, 평균=%dms, 최대=%dms, %s%n",
                scenario,
                requestCount,
                successCount,
                failures.size(),
                failureTypes(failures),
                wallClockMillis(run),
                totalMillis(run.results()),
                averageMillis(run.results()),
                maxMillis(run.results()),
                extra
        );

        writeConcurrencySummary(summary);
    }

    private void writeConcurrencySummary(String summary) {
        String summaryFile = System.getProperty("concurrency.summary.file");

        if (summaryFile == null || summaryFile.isBlank()) {
            System.out.print(summary);
            return;
        }

        try {
            Files.writeString(
                    Path.of(summaryFile),
                    summary,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND
            );
        } catch (java.io.IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private Map<String, Long> failureTypes(List<Throwable> failures) {
        Map<String, Long> failureTypes = new LinkedHashMap<>();

        for (Throwable failure : failures) {
            String failureType = failure.getClass().getSimpleName();
            failureTypes.put(failureType, failureTypes.getOrDefault(failureType, 0L) + 1);
        }

        return failureTypes;
    }

    private List<Integer> rangeClosed(int startInclusive, int endInclusive) {
        List<Integer> numbers = new ArrayList<>();

        for (int number = startInclusive; number <= endInclusive; number++) {
            numbers.add(number);
        }

        return numbers;
    }

    private long wallClockMillis(ConcurrentRun<?> run) {
        return TimeUnit.NANOSECONDS.toMillis(run.elapsedNanos());
    }

    private long totalMillis(List<? extends ConcurrentResult<?>> results) {
        return TimeUnit.NANOSECONDS.toMillis(
                results.stream()
                        .mapToLong(ConcurrentResult::durationNanos)
                        .sum()
        );
    }

    private long averageMillis(List<? extends ConcurrentResult<?>> results) {
        if (results.isEmpty()) {
            return 0;
        }

        return TimeUnit.NANOSECONDS.toMillis(
                (long) results.stream()
                        .mapToLong(ConcurrentResult::durationNanos)
                        .average()
                        .orElse(0)
        );
    }

    private long maxMillis(List<? extends ConcurrentResult<?>> results) {
        return TimeUnit.NANOSECONDS.toMillis(
                results.stream()
                        .mapToLong(ConcurrentResult::durationNanos)
                        .max()
                        .orElse(0)
        );
    }

    private Long createOpenRoom() {
        return roomRepository.save(Room.open()).getId();
    }

    private Long createActiveUser() {
        return userRepository.save(User.active()).getId();
    }

    private List<Long> createActiveUsers(int count) {
        List<Long> userIds = new ArrayList<>();

        for (int index = 0; index < count; index++) {
            userIds.add(createActiveUser());
        }

        return userIds;
    }

    @FunctionalInterface
    private interface ConcurrentTask<T> {

        T execute(int index);
    }

    private record ConcurrentRun<T>(
            List<ConcurrentResult<T>> results,
            long elapsedNanos
    ) {
    }

    private record ConcurrentResult<T>(
            T value,
            Throwable throwable,
            long durationNanos
    ) {

        static <T> ConcurrentResult<T> success(T value, long durationNanos) {
            return new ConcurrentResult<>(value, null, durationNanos);
        }

        static <T> ConcurrentResult<T> failure(Throwable throwable, long durationNanos) {
            return new ConcurrentResult<>(null, throwable, durationNanos);
        }

        boolean isSuccess() {
            return throwable == null;
        }
    }
}
