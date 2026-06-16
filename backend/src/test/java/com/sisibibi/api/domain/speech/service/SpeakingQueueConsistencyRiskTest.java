package com.sisibibi.api.domain.speech.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import com.sisibibi.api.domain.speech.config.SpeakingQueueProperties;
import com.sisibibi.api.domain.speech.dto.response.StageRequestRes;
import com.sisibibi.api.domain.speech.entity.SpeakingQueue;
import com.sisibibi.api.domain.speech.repository.RedisSpeakingQueueRepository;
import com.sisibibi.api.domain.user.repository.UserRepository;
import java.time.LocalDateTime;
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
class SpeakingQueueConsistencyRiskTest {

    private static final Long ROOM_ID = 1L;
    private static final Long USER_ID = 7L;
    private static final String QUEUE_KEY = "stage:queue:{1}";

    @Container
    static final GenericContainer<?> REDIS =
            new GenericContainer<>(DockerImageName.parse("redis:7.0-alpine"))
                    .withExposedPorts(6379);

    @Autowired
    private RedisSpeakingQueueRepository redisSpeakingQueueRepository;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
    }

    @BeforeEach
    void clearRedis() {
        redisTemplate.delete(QUEUE_KEY);
    }

    @Test
    void requestSpeakingTurn_doesNotCreateRedisEntryWhenRdbSaveFails() {
        SpeakingQueuePersistenceService failingPersistenceService =
                mock(SpeakingQueuePersistenceService.class);
        given(failingPersistenceService.createWaitingRequest(ROOM_ID, USER_ID))
                .willThrow(new IllegalStateException("database unavailable"));
        SpeakingQueueService service = new SpeakingQueueService(
                redisSpeakingQueueRepository,
                failingPersistenceService,
                mock(SpeakingQueueProperties.class),
                mock(UserRepository.class)
        );

        assertThatThrownBy(() -> service.requestSpeakingTurn(ROOM_ID, USER_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("database unavailable");

        assertThat(redisTemplate.opsForZSet().score(QUEUE_KEY, USER_ID.toString())).isNull();
    }

    @Test
    void requestSpeakingTurn_returnsDurableResultWhenRedisWriteFails() {
        SpeakingQueue saved = SpeakingQueue.waiting(
                ROOM_ID,
                USER_ID,
                15,
                LocalDateTime.of(2026, 6, 12, 11, 30)
        );
        SpeakingQueuePersistenceService persistenceService =
                mock(SpeakingQueuePersistenceService.class);
        given(persistenceService.createWaitingRequest(ROOM_ID, USER_ID)).willReturn(saved);
        RedisSpeakingQueueRepository failingRedisRepository =
                spy(redisSpeakingQueueRepository);
        doThrow(new IllegalStateException("redis unavailable"))
                .when(failingRedisRepository)
                .upsert(ROOM_ID, USER_ID, 15);
        SpeakingQueueService service = new SpeakingQueueService(
                failingRedisRepository,
                persistenceService,
                mock(SpeakingQueueProperties.class),
                mock(UserRepository.class)
        );

        StageRequestRes response = service.requestSpeakingTurn(ROOM_ID, USER_ID);

        assertThat(response.queueOrder()).isEqualTo(15);
        assertThat(redisTemplate.opsForZSet().score(QUEUE_KEY, USER_ID.toString())).isNull();
    }
}
