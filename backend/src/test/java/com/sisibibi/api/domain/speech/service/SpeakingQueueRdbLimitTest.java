package com.sisibibi.api.domain.speech.service;

import com.sisibibi.api.domain.room.entity.Room;
import com.sisibibi.api.domain.room.repository.RoomRepository;
import com.sisibibi.api.domain.speech.dto.response.CurrentSpeakerRes;
import com.sisibibi.api.domain.speech.dto.response.StageQueueRes;
import com.sisibibi.api.domain.speech.dto.response.StageRequestRes;
import com.sisibibi.api.domain.speech.entity.SpeakingQueueStatus;
import com.sisibibi.api.domain.user.entity.User;
import com.sisibibi.api.domain.user.repository.UserRepository;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
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

@SpringBootTest
@ActiveProfiles("test")
class SpeakingQueueRdbLimitTest {

    @Autowired
    private SpeakingQueueService speakingQueueService;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private UserRepository userRepository;

    @ParameterizedTest(name = "같은 방 동시 발언권 신청 한계 관찰: {0}건")
    @ValueSource(ints = {20, 50, 100})
    void hotRoomSpeakingRequests_observesRoomLockLimit(int requestCount) throws Exception {
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
                "같은 방 동시 발언권 신청",
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

    private List<Long> createActiveUsers(int count) {
        List<Long> userIds = new ArrayList<>();

        for (int index = 0; index < count; index++) {
            userIds.add(userRepository.save(User.active()).getId());
        }

        return userIds;
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
}
