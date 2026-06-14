package com.sisibibi.api.domain.speech.service;

import com.sisibibi.api.domain.room.entity.Room;
import com.sisibibi.api.domain.room.repository.RoomRepository;
import com.sisibibi.api.domain.speech.dto.response.StageCompleteRes;
import com.sisibibi.api.domain.speech.dto.response.CurrentSpeakerRes;
import com.sisibibi.api.domain.speech.dto.response.StageExpireRes;
import com.sisibibi.api.domain.speech.dto.response.StageQueueRes;
import com.sisibibi.api.domain.speech.dto.response.StageRequestRes;
import com.sisibibi.api.domain.speech.entity.SpeakingQueue;
import com.sisibibi.api.domain.speech.entity.SpeakingQueueStatus;
import com.sisibibi.api.domain.speech.repository.SpeakingQueueRepository;
import com.sisibibi.api.domain.user.entity.User;
import com.sisibibi.api.domain.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

abstract class SpeakingQueueRdbLimitTestSupport {

    @Autowired
    private SpeakingQueueService speakingQueueService;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private SpeakingQueueRepository speakingQueueRepository;

    @Autowired
    private UserRepository userRepository;

    @ParameterizedTest(name = "토픽 오픈 직후 신청 스파이크 관찰: {0}건")
    @ValueSource(ints = {20, 50, 100})
    void initialSpeakingRequestSpike_observesQueueOrderIssuanceLimit(int requestCount)
            throws Exception {
        clearSpeakingQueue();
        Long roomId = createOpenRoom();
        List<Long> userIds = createActiveUsers(requestCount);

        LimitRun<StageRequestRes> run = runConcurrently(
                requestCount,
                index -> speakingQueueService.requestSpeakingTurn(roomId, userIds.get(index))
        );

        List<StageRequestRes> successes = run.successes();
        List<Throwable> failures = run.failures();
        StageQueueRes waitingQueue = speakingQueueService.getWaitingQueue(roomId);
        CurrentSpeakerRes currentSpeaker = speakingQueueService.getCurrentSpeaker(roomId);

        printLimitSummary(
                "토픽 오픈 직후 신청 스파이크",
                requestCount,
                run,
                "assignedCount=1, waitingCount=" + waitingQueue.items().size()
                        + ", currentSpeakerUserId=" + currentSpeaker.userId()
        );

        assertThat(successes).hasSize(requestCount);
        assertThat(failures).isEmpty();
        assertThat(currentSpeaker.status()).isEqualTo(SpeakingQueueStatus.ASSIGNED);
        assertThat(waitingQueue.items()).hasSize(requestCount - 1);
        assertThat(successes)
                .extracting(StageRequestRes::queueOrder)
                .containsExactlyInAnyOrderElementsOf(rangeClosed(1, requestCount));
    }

    @ParameterizedTest(name = "자동 부여 한계 관찰: 대기자 {0}명")
    @ValueSource(ints = {10, 50, 100})
    void autoGrantNextSpeaker_observesWaitingQueueLookupLimit(int waitingCount) {
        clearSpeakingQueue();
        Long roomId = createOpenRoom();
        List<Long> userIds = createActiveUsers(waitingCount + 1);
        Long currentSpeakerUserId = userIds.get(0);
        Long expectedNextSpeakerUserId = userIds.get(1);

        for (Long userId : userIds) {
            speakingQueueService.requestSpeakingTurn(roomId, userId);
        }

        long startedAt = System.nanoTime();
        StageCompleteRes result = speakingQueueService.completeCurrentSpeaker(
                roomId,
                currentSpeakerUserId
        );
        long elapsedNanos = System.nanoTime() - startedAt;

        StageQueueRes waitingQueue = speakingQueueService.getWaitingQueue(roomId);
        CurrentSpeakerRes currentSpeaker = speakingQueueService.getCurrentSpeaker(roomId);

        printSingleOperationLimitSummary(
                "자동 부여",
                waitingCount,
                elapsedNanos,
                "completedUserId=" + result.completedSpeaker().userId()
                        + ", nextSpeakerUserId=" + result.nextSpeaker().userId()
                        + ", remainingWaitingCount=" + waitingQueue.items().size()
        );

        assertThat(result.completedSpeaker().status()).isEqualTo(SpeakingQueueStatus.COMPLETED);
        assertThat(result.completedSpeaker().userId()).isEqualTo(currentSpeakerUserId);
        assertThat(result.nextSpeaker()).isNotNull();
        assertThat(result.nextSpeaker().status()).isEqualTo(SpeakingQueueStatus.ASSIGNED);
        assertThat(result.nextSpeaker().userId()).isEqualTo(expectedNextSpeakerUserId);
        assertThat(currentSpeaker.userId()).isEqualTo(expectedNextSpeakerUserId);
        assertThat(waitingQueue.items()).hasSize(waitingCount - 1);
    }

    @ParameterizedTest(name = "자동 만료 후보 조회 한계 관찰: 후보 room {0}개")
    @ValueSource(ints = {10, 50, 100})
    void expirationCandidatePolling_observesCandidateLookupLimit(int candidateRoomCount) {
        clearSpeakingQueue();
        LocalDateTime assignedAt = LocalDateTime.now().minusMinutes(10);
        LocalDateTime expiresBefore = LocalDateTime.now().minusMinutes(5);
        List<Long> roomIds = createExpiredAssignedQueues(candidateRoomCount, assignedAt);

        long startedAt = System.nanoTime();
        List<Long> candidateRoomIds = speakingQueueRepository
                .findDistinctRoomIdsByStatusAndAssignedAtLessThanEqual(
                        SpeakingQueueStatus.ASSIGNED,
                        expiresBefore
                );
        long elapsedNanos = System.nanoTime() - startedAt;

        printCandidateLookupLimitSummary(
                "자동 만료 후보 조회",
                candidateRoomCount,
                candidateRoomIds.size(),
                elapsedNanos
        );

        assertThat(candidateRoomIds).containsExactlyInAnyOrderElementsOf(roomIds);
    }

    @ParameterizedTest(name = "자동 만료 처리 한계 관찰: 후보 room {0}개")
    @ValueSource(ints = {10, 50, 100})
    void expireTimedOutSpeakers_observesExpirationProcessingLimit(int candidateRoomCount) {
        clearSpeakingQueue();
        List<ExpirationRoomFixture> fixtures = createExpirationRoomFixtures(candidateRoomCount);
        LocalDateTime schedulerNow = LocalDateTime.now().plusMinutes(10);

        long startedAt = System.nanoTime();
        List<StageExpireRes> results = new ArrayList<>();

        for (ExpirationRoomFixture fixture : fixtures) {
            results.add(speakingQueueService.expireCurrentSpeakerIfTimedOut(
                    fixture.roomId(),
                    schedulerNow,
                    Duration.ofMinutes(5)
            ));
        }

        long elapsedNanos = System.nanoTime() - startedAt;

        printBatchOperationLimitSummary(
                "자동 만료 처리",
                candidateRoomCount,
                elapsedNanos,
                "success=" + results.size() + ", failure=0"
        );

        assertThat(results).hasSize(candidateRoomCount);

        for (int index = 0; index < candidateRoomCount; index++) {
            StageExpireRes result = results.get(index);
            ExpirationRoomFixture fixture = fixtures.get(index);
            StageQueueRes waitingQueue = speakingQueueService.getWaitingQueue(fixture.roomId());
            CurrentSpeakerRes currentSpeaker =
                    speakingQueueService.getCurrentSpeaker(fixture.roomId());

            assertThat(result.expiredSpeaker().userId()).isEqualTo(fixture.currentSpeakerUserId());
            assertThat(result.expiredSpeaker().status()).isEqualTo(SpeakingQueueStatus.EXPIRED);
            assertThat(result.nextSpeaker()).isNotNull();
            assertThat(result.nextSpeaker().userId()).isEqualTo(fixture.nextSpeakerUserId());
            assertThat(result.nextSpeaker().status()).isEqualTo(SpeakingQueueStatus.ASSIGNED);
            assertThat(currentSpeaker.userId()).isEqualTo(fixture.nextSpeakerUserId());
            assertThat(waitingQueue.items()).isEmpty();
        }
    }

    @Test
    void completeAndExpireRaceRepeated_observesTerminalTransitionCost() throws Exception {
        clearSpeakingQueue();
        int repetitionCount = 100;
        List<RaceIterationResult> iterationResults = new ArrayList<>();

        for (int index = 0; index < repetitionCount; index++) {
            iterationResults.add(runCompleteAndExpireRaceOnce());
        }

        printRaceLimitSummary(
                "종료 vs 자동 만료 경합 반복",
                repetitionCount,
                iterationResults
        );

        assertThat(iterationResults).hasSize(repetitionCount);
        assertThat(iterationResults)
                .extracting(RaceIterationResult::successTransitionCount)
                .containsOnly(1);
        assertThat(iterationResults)
                .extracting(RaceIterationResult::failedRequestCount)
                .containsOnly(1);
        assertThat(iterationResults)
                .extracting(RaceIterationResult::terminalStatus)
                .allSatisfy(status -> assertThat(status)
                        .isIn(SpeakingQueueStatus.COMPLETED, SpeakingQueueStatus.EXPIRED));
    }

    private <T> LimitRun<T> runConcurrently(
            int taskCount,
            LimitTask<T> task
    ) throws Exception {
        ExecutorService executorService = Executors.newFixedThreadPool(taskCount);
        CountDownLatch ready = new CountDownLatch(taskCount);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<LimitResult<T>>> futures = new ArrayList<>();

        try {
            for (int index = 0; index < taskCount; index++) {
                int taskIndex = index;
                futures.add(executorService.submit(toCallable(taskIndex, ready, start, task)));
            }

            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            long startedAt = System.nanoTime();
            start.countDown();

            List<LimitResult<T>> results = new ArrayList<>();
            for (Future<LimitResult<T>> future : futures) {
                results.add(future.get(30, TimeUnit.SECONDS));
            }

            return new LimitRun<>(results, System.nanoTime() - startedAt);
        } finally {
            executorService.shutdownNow();
        }
    }

    private <T> Callable<LimitResult<T>> toCallable(
            int index,
            CountDownLatch ready,
            CountDownLatch start,
            LimitTask<T> task
    ) {
        return () -> {
            ready.countDown();
            start.await();
            long startedAt = System.nanoTime();

            try {
                T value = task.execute(index);
                return LimitResult.success(value, System.nanoTime() - startedAt);
            } catch (Throwable throwable) {
                return LimitResult.failure(throwable, System.nanoTime() - startedAt);
            }
        };
    }

    private void printLimitSummary(
            String scenario,
            int requestCount,
            LimitRun<?> run,
            String extra
    ) {
        LimitStats stats = LimitStats.from(run.results());
        String summary = String.format(
                "[RDB LIMIT TEST] scenario=%s, requests=%d, success=%d, failure=%d, "
                        + "failureTypes=%s, totalElapsedMs=%d, avgLatencyMs=%d, "
                        + "minLatencyMs=%d, maxLatencyMs=%d, p50LatencyMs=%d, "
                        + "p95LatencyMs=%d, %s%n",
                scenario,
                requestCount,
                run.successes().size(),
                run.failures().size(),
                failureTypes(run.failures()),
                TimeUnit.NANOSECONDS.toMillis(run.elapsedNanos()),
                stats.averageMillis(),
                stats.minMillis(),
                stats.maxMillis(),
                stats.p50Millis(),
                stats.p95Millis(),
                extra
        );

        writeSummary(summary);
    }

    private void printSingleOperationLimitSummary(
            String scenario,
            int waitingCount,
            long elapsedNanos,
            String extra
    ) {
        String summary = String.format(
                "[RDB LIMIT TEST] scenario=%s, waitingCount=%d, elapsedMs=%d, %s%n",
                scenario,
                waitingCount,
                TimeUnit.NANOSECONDS.toMillis(elapsedNanos),
                extra
        );

        writeSummary(summary);
    }

    private void printCandidateLookupLimitSummary(
            String scenario,
            int candidateRoomCount,
            int foundRoomCount,
            long elapsedNanos
    ) {
        String summary = String.format(
                "[RDB LIMIT TEST] scenario=%s, candidateRoomCount=%d, "
                        + "foundRoomCount=%d, elapsedMs=%d%n",
                scenario,
                candidateRoomCount,
                foundRoomCount,
                TimeUnit.NANOSECONDS.toMillis(elapsedNanos)
        );

        writeSummary(summary);
    }

    private void printBatchOperationLimitSummary(
            String scenario,
            int candidateRoomCount,
            long elapsedNanos,
            String extra
    ) {
        long totalElapsedMillis = TimeUnit.NANOSECONDS.toMillis(elapsedNanos);
        long averagePerRoomMillis = candidateRoomCount == 0
                ? 0
                : totalElapsedMillis / candidateRoomCount;
        String summary = String.format(
                "[RDB LIMIT TEST] scenario=%s, candidateRoomCount=%d, "
                        + "totalElapsedMs=%d, avgPerRoomMs=%d, %s%n",
                scenario,
                candidateRoomCount,
                totalElapsedMillis,
                averagePerRoomMillis,
                extra
        );

        writeSummary(summary);
    }

    private void printRaceLimitSummary(
            String scenario,
            int repetitionCount,
            List<RaceIterationResult> results
    ) {
        RaceStats stats = RaceStats.from(results);
        String summary = String.format(
                "[RDB LIMIT TEST] scenario=%s, repetitions=%d, "
                        + "successTransitions=%d, failedRequests=%d, "
                        + "completedTransitions=%d, expiredTransitions=%d, "
                        + "failureTypes=%s, totalElapsedMs=%d, avgIterationMs=%d, "
                        + "maxIterationMs=%d, p95IterationMs=%d%n",
                scenario,
                repetitionCount,
                results.stream().mapToInt(RaceIterationResult::successTransitionCount).sum(),
                results.stream().mapToInt(RaceIterationResult::failedRequestCount).sum(),
                countTerminalStatus(results, SpeakingQueueStatus.COMPLETED),
                countTerminalStatus(results, SpeakingQueueStatus.EXPIRED),
                failureTypes(results.stream()
                        .flatMap(result -> result.failures().stream())
                        .toList()),
                stats.totalElapsedMillis(),
                stats.averageIterationMillis(),
                stats.maxIterationMillis(),
                stats.p95IterationMillis()
        );

        writeSummary(summary);
    }

    private void writeSummary(String summary) {
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
        } catch (IOException e) {
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

    private long countTerminalStatus(
            List<RaceIterationResult> results,
            SpeakingQueueStatus status
    ) {
        return results.stream()
                .filter(result -> result.terminalStatus() == status)
                .count();
    }

    private List<Integer> rangeClosed(int startInclusive, int endInclusive) {
        List<Integer> numbers = new ArrayList<>();

        for (int number = startInclusive; number <= endInclusive; number++) {
            numbers.add(number);
        }

        return numbers;
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
            userIds.add(userRepository.save(User.active()).getId());
        }

        return userIds;
    }

    private List<Long> createExpiredAssignedQueues(
            int candidateRoomCount,
            LocalDateTime assignedAt
    ) {
        List<Long> roomIds = new ArrayList<>();

        for (int index = 0; index < candidateRoomCount; index++) {
            Long roomId = createOpenRoom();
            Long userId = createActiveUser();
            SpeakingQueue speakingQueue = SpeakingQueue.create(
                    roomId,
                    userId,
                    1,
                    assignedAt.minusMinutes(1)
            );
            speakingQueue.assign(assignedAt);
            speakingQueueRepository.save(speakingQueue);
            roomIds.add(roomId);
        }

        return roomIds;
    }

    private List<ExpirationRoomFixture> createExpirationRoomFixtures(int candidateRoomCount) {
        List<ExpirationRoomFixture> fixtures = new ArrayList<>();

        for (int index = 0; index < candidateRoomCount; index++) {
            Long roomId = createOpenRoom();
            Long currentSpeakerUserId = createActiveUser();
            Long nextSpeakerUserId = createActiveUser();

            speakingQueueService.requestSpeakingTurn(roomId, currentSpeakerUserId);
            speakingQueueService.requestSpeakingTurn(roomId, nextSpeakerUserId);

            fixtures.add(new ExpirationRoomFixture(
                    roomId,
                    currentSpeakerUserId,
                    nextSpeakerUserId
            ));
        }

        return fixtures;
    }

    private RaceIterationResult runCompleteAndExpireRaceOnce() throws Exception {
        Long roomId = createOpenRoom();
        Long currentSpeakerUserId = createActiveUser();
        Long nextSpeakerUserId = createActiveUser();

        speakingQueueService.requestSpeakingTurn(roomId, currentSpeakerUserId);
        speakingQueueService.requestSpeakingTurn(roomId, nextSpeakerUserId);

        LocalDateTime schedulerNow = LocalDateTime.now();
        long startedAt = System.nanoTime();

        LimitRun<Object> run = runConcurrently(
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
                            Duration.ZERO
                    );
                }
        );

        long elapsedNanos = System.nanoTime() - startedAt;
        StageRequestRes terminalRequest =
                speakingQueueService.getMyRequest(roomId, currentSpeakerUserId);
        StageRequestRes nextSpeakerRequest =
                speakingQueueService.getMyRequest(roomId, nextSpeakerUserId);
        CurrentSpeakerRes currentSpeaker = speakingQueueService.getCurrentSpeaker(roomId);
        StageQueueRes waitingQueue = speakingQueueService.getWaitingQueue(roomId);

        assertThat(run.successes()).hasSize(1);
        assertThat(run.failures()).hasSize(1);
        assertThat(terminalRequest.status())
                .isIn(SpeakingQueueStatus.COMPLETED, SpeakingQueueStatus.EXPIRED);
        assertThat(nextSpeakerRequest.status()).isEqualTo(SpeakingQueueStatus.ASSIGNED);
        assertThat(currentSpeaker.userId()).isEqualTo(nextSpeakerUserId);
        assertThat(waitingQueue.items()).isEmpty();

        return new RaceIterationResult(
                terminalRequest.status(),
                run.successes().size(),
                run.failures().size(),
                run.failures(),
                elapsedNanos
        );
    }

    private void clearSpeakingQueue() {
        speakingQueueRepository.deleteAll();
    }

    @FunctionalInterface
    private interface LimitTask<T> {

        T execute(int index);
    }

    private record LimitRun<T>(
            List<LimitResult<T>> results,
            long elapsedNanos
    ) {

        List<T> successes() {
            return results.stream()
                    .filter(LimitResult::isSuccess)
                    .map(LimitResult::value)
                    .toList();
        }

        List<Throwable> failures() {
            return results.stream()
                    .filter(result -> !result.isSuccess())
                    .map(LimitResult::throwable)
                    .toList();
        }
    }

    private record LimitResult<T>(
            T value,
            Throwable throwable,
            long durationNanos
    ) {

        static <T> LimitResult<T> success(T value, long durationNanos) {
            return new LimitResult<>(value, null, durationNanos);
        }

        static <T> LimitResult<T> failure(Throwable throwable, long durationNanos) {
            return new LimitResult<>(null, throwable, durationNanos);
        }

        boolean isSuccess() {
            return throwable == null;
        }
    }

    private record LimitStats(
            long averageMillis,
            long minMillis,
            long maxMillis,
            long p50Millis,
            long p95Millis
    ) {

        static LimitStats from(List<? extends LimitResult<?>> results) {
            List<Long> durations = results.stream()
                    .map(LimitResult::durationNanos)
                    .sorted()
                    .toList();

            if (durations.isEmpty()) {
                return new LimitStats(0, 0, 0, 0, 0);
            }

            long sum = durations.stream().mapToLong(Long::longValue).sum();

            return new LimitStats(
                    TimeUnit.NANOSECONDS.toMillis(sum / durations.size()),
                    TimeUnit.NANOSECONDS.toMillis(durations.get(0)),
                    TimeUnit.NANOSECONDS.toMillis(durations.get(durations.size() - 1)),
                    TimeUnit.NANOSECONDS.toMillis(percentile(durations, 50)),
                    TimeUnit.NANOSECONDS.toMillis(percentile(durations, 95))
            );
        }

        private static long percentile(List<Long> sortedValues, int percentile) {
            int index = (int) Math.ceil(sortedValues.size() * (percentile / 100.0)) - 1;
            int safeIndex = Math.max(0, Math.min(index, sortedValues.size() - 1));

            return sortedValues.get(safeIndex);
        }
    }

    private record ExpirationRoomFixture(
            Long roomId,
            Long currentSpeakerUserId,
            Long nextSpeakerUserId
    ) {
    }

    private record RaceIterationResult(
            SpeakingQueueStatus terminalStatus,
            int successTransitionCount,
            int failedRequestCount,
            List<Throwable> failures,
            long elapsedNanos
    ) {
    }

    private record RaceStats(
            long totalElapsedMillis,
            long averageIterationMillis,
            long maxIterationMillis,
            long p95IterationMillis
    ) {

        static RaceStats from(List<RaceIterationResult> results) {
            List<Long> durations = results.stream()
                    .map(RaceIterationResult::elapsedNanos)
                    .sorted()
                    .toList();

            if (durations.isEmpty()) {
                return new RaceStats(0, 0, 0, 0);
            }

            long sum = durations.stream().mapToLong(Long::longValue).sum();

            return new RaceStats(
                    TimeUnit.NANOSECONDS.toMillis(sum),
                    TimeUnit.NANOSECONDS.toMillis(sum / durations.size()),
                    TimeUnit.NANOSECONDS.toMillis(durations.get(durations.size() - 1)),
                    TimeUnit.NANOSECONDS.toMillis(percentile(durations, 95))
            );
        }

        private static long percentile(List<Long> sortedValues, int percentile) {
            int index = (int) Math.ceil(sortedValues.size() * (percentile / 100.0)) - 1;
            int safeIndex = Math.max(0, Math.min(index, sortedValues.size() - 1));

            return sortedValues.get(safeIndex);
        }
    }
}
