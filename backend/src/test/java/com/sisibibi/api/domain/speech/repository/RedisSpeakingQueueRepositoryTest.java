package com.sisibibi.api.domain.speech.repository;

import static org.assertj.core.api.Assertions.assertThat;

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
    }

    @Test
    void upsert_storesQueueOrderPersistedByRdb() {
        speakingQueueRepository.upsert(1L, 10L, 15);
        speakingQueueRepository.upsert(1L, 20L, 21);

        assertThat(redisTemplate.opsForZSet().score(QUEUE_KEY, "10")).isEqualTo(15.0);
        assertThat(redisTemplate.opsForZSet().score(QUEUE_KEY, "20")).isEqualTo(21.0);
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
    }

    @Test
    void assign_removesUserFromQueueAndStoresCurrentSpeaker() {
        speakingQueueRepository.upsert(1L, 10L, 15);

        speakingQueueRepository.assign(1L, 10L);

        assertThat(redisTemplate.opsForZSet().score(QUEUE_KEY, "10")).isNull();
        assertThat(redisTemplate.opsForValue().get("stage:current:{1}")).isEqualTo("10");
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
    }

    @Test
    void removeCurrentSpeaker_keepsNewCurrentSpeaker() {
        redisTemplate.opsForValue().set(CURRENT_SPEAKER_KEY, "20");

        speakingQueueRepository.removeCurrentSpeaker(1L, 10L);

        assertThat(redisTemplate.opsForValue().get(CURRENT_SPEAKER_KEY)).isEqualTo("20");
    }
}
