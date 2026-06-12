package com.sisibibi.api.domain.speech.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.OptionalInt;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.redis.DataRedisTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@DataRedisTest
@Testcontainers(disabledWithoutDocker = true)
@Import(RedisSpeakingQueueRepository.class)
class RedisSpeakingQueueRepositoryTest {

    private static final String QUEUE_KEY = "stage:queue:{1}";
    private static final String SEQUENCE_KEY = "stage:queue:{1}:sequence";

    @Container
    static final GenericContainer<?> REDIS =
            new GenericContainer<>(DockerImageName.parse("redis:7.0-alpine"))
                    .withExposedPorts(6379);

    @Autowired
    private RedisSpeakingQueueRepository speakingQueueRepository;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
    }

    @BeforeEach
    void clearQueue() {
        redisTemplate.delete(QUEUE_KEY);
        redisTemplate.delete(SEQUENCE_KEY);
    }

    @Test
    void enqueue_assignsQueueOrdersInRequestOrder() {
        OptionalInt firstQueueOrder = speakingQueueRepository.enqueue(1L, 10L);
        OptionalInt secondQueueOrder = speakingQueueRepository.enqueue(1L, 20L);

        assertThat(firstQueueOrder).hasValue(1);
        assertThat(secondQueueOrder).hasValue(2);
    }

    @Test
    void enqueue_doesNotReuseQueueOrderAfterEarlierUserLeaves() {
        speakingQueueRepository.enqueue(1L, 10L);
        speakingQueueRepository.enqueue(1L, 20L);
        redisTemplate.opsForZSet().remove(QUEUE_KEY, "10");

        OptionalInt thirdQueueOrder = speakingQueueRepository.enqueue(1L, 30L);

        assertThat(thirdQueueOrder).hasValue(3);
    }

    @Test
    void enqueue_returnsDuplicateResult_whenUserAlreadyWaitsInRoom() {
        speakingQueueRepository.enqueue(1L, 10L);

        OptionalInt duplicateQueueOrder = speakingQueueRepository.enqueue(1L, 10L);

        assertThat(duplicateQueueOrder).isEmpty();
        assertThat(redisTemplate.opsForZSet().size(QUEUE_KEY)).isEqualTo(1L);
    }

    @Test
    void remove_deletesUserFromWaitingQueue() {
        speakingQueueRepository.enqueue(1L, 10L);

        speakingQueueRepository.remove(1L, 10L);

        assertThat(redisTemplate.opsForZSet().score(QUEUE_KEY, "10")).isNull();
    }

    @Test
    void enqueue_assignsUniqueQueueOrders_whenUsersRequestConcurrently() throws Exception {
        int requestCount = 20;
        CountDownLatch startGate = new CountDownLatch(1);
        List<Future<OptionalInt>> futures = new ArrayList<>();

        try (ExecutorService executor = Executors.newFixedThreadPool(requestCount)) {
            for (long userId = 1L; userId <= requestCount; userId++) {
                long requestedUserId = userId;
                futures.add(executor.submit(() -> {
                    startGate.await();
                    return speakingQueueRepository.enqueue(1L, requestedUserId);
                }));
            }

            startGate.countDown();

            List<Integer> queueOrders = new ArrayList<>();
            for (Future<OptionalInt> future : futures) {
                queueOrders.add(future.get().orElseThrow());
            }
            Collections.sort(queueOrders);

            assertThat(queueOrders).containsExactlyElementsOf(
                    java.util.stream.IntStream.rangeClosed(1, requestCount).boxed().toList()
            );
            assertThat(redisTemplate.opsForZSet().size(QUEUE_KEY)).isEqualTo(requestCount);
        }
    }

    @Test
    void enqueue_registersUserOnlyOnce_whenDuplicateRequestsArriveConcurrently() throws Exception {
        int requestCount = 10;
        CountDownLatch startGate = new CountDownLatch(1);
        List<Future<OptionalInt>> futures = new ArrayList<>();

        try (ExecutorService executor = Executors.newFixedThreadPool(requestCount)) {
            for (int i = 0; i < requestCount; i++) {
                futures.add(executor.submit(() -> {
                    startGate.await();
                    return speakingQueueRepository.enqueue(1L, 10L);
                }));
            }

            startGate.countDown();

            List<OptionalInt> results = new ArrayList<>();
            for (Future<OptionalInt> future : futures) {
                results.add(future.get());
            }

            assertThat(results.stream().filter(OptionalInt::isPresent).count()).isEqualTo(1L);
            assertThat(results.stream()
                    .filter(OptionalInt::isPresent)
                    .mapToInt(OptionalInt::getAsInt)
                    .findFirst()
            ).hasValue(1);
            assertThat(results.stream().filter(OptionalInt::isEmpty).count())
                    .isEqualTo(requestCount - 1L);
            assertThat(redisTemplate.opsForZSet().size(QUEUE_KEY)).isEqualTo(1L);
        }
    }
}
