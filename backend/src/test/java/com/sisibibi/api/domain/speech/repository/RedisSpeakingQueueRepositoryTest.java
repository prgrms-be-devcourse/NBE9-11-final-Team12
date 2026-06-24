package com.sisibibi.api.domain.speech.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sisibibi.api.domain.speech.entity.SpeakingQueue;
import com.sisibibi.api.domain.speech.entity.SpeechStance;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
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
@Testcontainers
@Import(RedisSpeakingQueueRepository.class)
class RedisSpeakingQueueRepositoryTest {

    private static final String QUEUE_KEY = "stage:queue:{1}";
    private static final String CURRENT_SPEAKER_KEY = "stage:current:{1}";
    private static final String PROJECTION_VERSION_KEY = "stage:projection-version:{1}";

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
        redisTemplate.delete(CURRENT_SPEAKER_KEY);
        redisTemplate.delete(PROJECTION_VERSION_KEY);
    }

    @Test
    void upsert_storesQueueOrderPersistedByRdb() {
        speakingQueueRepository.upsert(1L, 10L, 15);
        speakingQueueRepository.upsert(1L, 20L, 21);

        assertThat(redisTemplate.opsForZSet().score(QUEUE_KEY, "10")).isEqualTo(15.0);
        assertThat(redisTemplate.opsForZSet().score(QUEUE_KEY, "20")).isEqualTo(21.0);
        assertThat(speakingQueueRepository.currentProjectionVersion(1L)).isEqualTo(2L);
    }

    @Test
    void upsert_isIdempotentForSameUserAndOrder() {
        speakingQueueRepository.upsert(1L, 10L, 15);

        speakingQueueRepository.upsert(1L, 10L, 15);

        assertThat(redisTemplate.opsForZSet().size(QUEUE_KEY)).isEqualTo(1L);
        assertThat(redisTemplate.opsForZSet().score(QUEUE_KEY, "10")).isEqualTo(15.0);
    }

    @Test
    void remove_deletesUserFromWaitingQueue() {
        speakingQueueRepository.upsert(1L, 10L, 15);

        speakingQueueRepository.remove(1L, 10L);

        assertThat(redisTemplate.opsForZSet().score(QUEUE_KEY, "10")).isNull();
        assertThat(speakingQueueRepository.currentProjectionVersion(1L)).isEqualTo(2L);
    }

    @Test
    void rank_returnsOneBasedWaitingRank() {
        speakingQueueRepository.upsert(1L, 10L, 15);
        speakingQueueRepository.upsert(1L, 20L, 21);

        assertThat(speakingQueueRepository.rank(1L, 10L)).contains(1);
        assertThat(speakingQueueRepository.rank(1L, 20L)).contains(2);
    }

    @Test
    void rank_returnsEmptyWhenUserIsNotWaiting() {
        assertThat(speakingQueueRepository.rank(1L, 10L)).isEmpty();
    }

    @Test
    void findWaitingUserIds_returnsUsersByWaitingOrder() {
        speakingQueueRepository.upsert(1L, 30L, 30);
        speakingQueueRepository.upsert(1L, 10L, 10);
        speakingQueueRepository.upsert(1L, 20L, 20);

        assertThat(speakingQueueRepository.findWaitingUserIds(1L, 0, 1))
                .containsExactly(10L, 20L);
    }

    @Test
    void findWaitingUserIds_returnsEmptyWhenQueueDoesNotExist() {
        assertThat(speakingQueueRepository.findWaitingUserIds(1L, 0, 4))
                .isEmpty();
    }

    @Test
    void count_returnsWaitingQueueSize() {
        speakingQueueRepository.upsert(1L, 10L, 10);
        speakingQueueRepository.upsert(1L, 20L, 20);

        assertThat(speakingQueueRepository.count(1L)).isEqualTo(2L);
    }

    @Test
    void count_returnsZeroWhenQueueDoesNotExist() {
        assertThat(speakingQueueRepository.count(1L)).isZero();
    }

    @Test
    void assign_removesUserFromQueueAndStoresCurrentSpeaker() {
        speakingQueueRepository.upsert(1L, 10L, 15);

        speakingQueueRepository.assign(1L, 10L);

        assertThat(redisTemplate.opsForZSet().score(QUEUE_KEY, "10")).isNull();
        assertThat(redisTemplate.opsForValue().get("stage:current:{1}")).isEqualTo("10");
        assertThat(speakingQueueRepository.currentProjectionVersion(1L)).isEqualTo(2L);
    }

    @Test
    void assign_isIdempotentForSameCurrentSpeaker() {
        speakingQueueRepository.upsert(1L, 10L, 15);

        speakingQueueRepository.assign(1L, 10L);
        speakingQueueRepository.assign(1L, 10L);

        assertThat(redisTemplate.opsForZSet().score(QUEUE_KEY, "10")).isNull();
        assertThat(redisTemplate.opsForValue().get(CURRENT_SPEAKER_KEY)).isEqualTo("10");
    }

    @Test
    void removeCurrentSpeaker_deletesMatchingCurrentSpeaker() {
        redisTemplate.opsForValue().set(CURRENT_SPEAKER_KEY, "10");

        speakingQueueRepository.removeCurrentSpeaker(1L, 10L);

        assertThat(redisTemplate.opsForValue().get(CURRENT_SPEAKER_KEY)).isNull();
        assertThat(speakingQueueRepository.currentProjectionVersion(1L)).isEqualTo(1L);
    }

    @Test
    void removeCurrentSpeaker_keepsNewCurrentSpeaker() {
        redisTemplate.opsForValue().set(CURRENT_SPEAKER_KEY, "20");

        speakingQueueRepository.removeCurrentSpeaker(1L, 10L);

        assertThat(redisTemplate.opsForValue().get(CURRENT_SPEAKER_KEY)).isEqualTo("20");
        assertThat(speakingQueueRepository.currentProjectionVersion(1L)).isZero();
    }

    @Test
    void replaceRoomProjectionIfVersionMatches_rebuildsWaitingQueueAndCurrentSpeaker() {
        speakingQueueRepository.upsert(1L, 10L, 1);
        speakingQueueRepository.upsert(1L, 20L, 2);
        redisTemplate.opsForValue().set(CURRENT_SPEAKER_KEY, "10");
        long expectedVersion = speakingQueueRepository.currentProjectionVersion(1L);
        SpeakingQueue waiting = waitingRequest(1L, 30L, 3);
        SpeakingQueue currentSpeaker = assignedRequest(1L, 40L, 4);

        boolean replaced = speakingQueueRepository.replaceRoomProjectionIfVersionMatches(
                1L,
                List.of(waiting),
                Optional.of(currentSpeaker),
                expectedVersion
        );

        assertThat(replaced).isTrue();
        assertThat(redisTemplate.opsForZSet().score(QUEUE_KEY, "10")).isNull();
        assertThat(redisTemplate.opsForZSet().score(QUEUE_KEY, "20")).isNull();
        assertThat(redisTemplate.opsForZSet().score(QUEUE_KEY, "30")).isEqualTo(3.0);
        assertThat(redisTemplate.opsForValue().get(CURRENT_SPEAKER_KEY)).isEqualTo("40");
        assertThat(speakingQueueRepository.currentProjectionVersion(1L))
                .isEqualTo(expectedVersion + 1);
    }

    @Test
    void replaceRoomProjectionIfVersionMatches_recoversCorruptedProjectionVersion() {
        redisTemplate.opsForValue().set(PROJECTION_VERSION_KEY, "corrupted");
        SpeakingQueue waiting = waitingRequest(1L, 30L, 3);

        boolean replaced = speakingQueueRepository.replaceRoomProjectionIfVersionMatches(
                1L,
                List.of(waiting),
                Optional.empty(),
                speakingQueueRepository.currentProjectionVersion(1L)
        );

        assertThat(replaced).isTrue();
        assertThat(redisTemplate.opsForZSet().score(QUEUE_KEY, "30")).isEqualTo(3.0);
        assertThat(redisTemplate.opsForValue().get(PROJECTION_VERSION_KEY)).isEqualTo("1");
        assertThat(speakingQueueRepository.currentProjectionVersion(1L)).isEqualTo(1L);
    }

    @Test
    void replaceRoomProjectionIfVersionMatches_clearsCurrentSpeakerWhenRdbHasNoAssignedSpeaker() {
        redisTemplate.opsForValue().set(CURRENT_SPEAKER_KEY, "10");
        long expectedVersion = speakingQueueRepository.currentProjectionVersion(1L);

        boolean replaced = speakingQueueRepository.replaceRoomProjectionIfVersionMatches(
                1L,
                List.of(),
                Optional.empty(),
                expectedVersion
        );

        assertThat(replaced).isTrue();
        assertThat(redisTemplate.opsForZSet().size(QUEUE_KEY)).isZero();
        assertThat(redisTemplate.opsForValue().get(CURRENT_SPEAKER_KEY)).isNull();
    }

    @Test
    void replaceRoomProjectionIfVersionMatches_rejectsWaitingQueueWithoutQueueOrder() {
        SpeakingQueue waitingWithoutQueueOrder = SpeakingQueue.waiting(
                1L,
                30L,
                SpeechStance.PRO,
                LocalDateTime.of(2026, 6, 12, 11, 30)
        );

        assertThatThrownBy(() -> speakingQueueRepository.replaceRoomProjectionIfVersionMatches(
                1L,
                List.of(waitingWithoutQueueOrder),
                Optional.empty(),
                0L
        ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Waiting speaking queue must have queue order")
                .hasMessageContaining("roomId=1")
                .hasMessageContaining("userId=30");

        assertThat(redisTemplate.hasKey(QUEUE_KEY)).isFalse();
        assertThat(redisTemplate.hasKey(CURRENT_SPEAKER_KEY)).isFalse();
    }

    @Test
    void replaceRoomProjectionIfVersionMatches_rejectsStaleProjection() {
        speakingQueueRepository.upsert(1L, 10L, 1);
        SpeakingQueue waiting = waitingRequest(1L, 20L, 2);

        boolean replaced = speakingQueueRepository.replaceRoomProjectionIfVersionMatches(
                1L,
                List.of(waiting),
                Optional.empty(),
                0L
        );

        assertThat(replaced).isFalse();
        assertThat(redisTemplate.opsForZSet().score(QUEUE_KEY, "10")).isEqualTo(1.0);
        assertThat(redisTemplate.opsForZSet().score(QUEUE_KEY, "20")).isNull();
        assertThat(speakingQueueRepository.currentProjectionVersion(1L)).isEqualTo(1L);
    }

    private SpeakingQueue waitingRequest(Long roomId, Long userId, int queueOrder) {
        return SpeakingQueue.waiting(
                roomId,
                userId,
                queueOrder,
                SpeechStance.PRO,
                LocalDateTime.of(2026, 6, 12, 11, 30)
        );
    }

    private SpeakingQueue assignedRequest(Long roomId, Long userId, int queueOrder) {
        SpeakingQueue speakingQueue = waitingRequest(roomId, userId, queueOrder);
        speakingQueue.assign(
                LocalDateTime.of(2026, 6, 12, 11, 31),
                LocalDateTime.of(2026, 6, 12, 11, 34)
        );
        return speakingQueue;
    }
}
