package com.sisibibi.api.global.websocket.presence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.core.script.RedisScript;

class RedisRoomPresenceRepositoryTest {

    private static final String PRESENCE_KEY = "room:presence:{1}:2";
    private static final String EXPIRATIONS_KEY = "room:presence:expirations";
    private static final String EXPIRATION_FAILURES_KEY = "room:presence:expiration-failures";

    private StringRedisTemplate redisTemplate;
    private ZSetOperations<String, String> zSetOperations;
    private HashOperations<String, Object, Object> hashOperations;
    private RedisRoomPresenceRepository repository;

    @BeforeEach
    void setUp() {
        redisTemplate = mock(StringRedisTemplate.class);
        zSetOperations = mock(ZSetOperations.class);
        hashOperations = mock(HashOperations.class);
        given(redisTemplate.opsForZSet()).willReturn(zSetOperations);
        given(redisTemplate.opsForHash()).willReturn(hashOperations);
        repository = new RedisRoomPresenceRepository(redisTemplate);
    }

    @Test
    void markConnected_returnsZero_whenRedisScriptReturnsNull() {
        given(redisTemplate.execute(
                any(RedisScript.class),
                anyList(),
                any(),
                any()
        )).willReturn(null);

        long generation = repository.markConnected(
                1L,
                2L,
                "session-1",
                Instant.parse("2026-06-28T01:00:00Z")
        );

        assertThat(generation).isZero();
    }

    @Test
    void markConnected_returnsGeneration_whenRedisScriptReturnsValue() {
        given(redisTemplate.execute(
                any(RedisScript.class),
                anyList(),
                any(),
                any()
        )).willReturn(7L);

        long generation = repository.markConnected(
                1L,
                2L,
                "session-1",
                Instant.parse("2026-06-28T01:00:00Z")
        );

        assertThat(generation).isEqualTo(7L);
    }

    @Test
    void markDisconnectedIfCurrentSession_returnsFalse_whenRedisScriptReturnsNull() {
        given(redisTemplate.execute(
                any(RedisScript.class),
                anyList(),
                any(),
                any(),
                any(),
                any()
        )).willReturn(null);

        boolean disconnected = repository.markDisconnectedIfCurrentSession(
                1L,
                2L,
                "session-1",
                Instant.parse("2026-06-28T01:00:00Z"),
                Instant.parse("2026-06-28T01:01:00Z")
        );

        assertThat(disconnected).isFalse();
    }

    @Test
    void markDisconnectedIfCurrentSession_returnsFalse_whenRedisScriptReturnsZero() {
        given(redisTemplate.execute(
                any(RedisScript.class),
                anyList(),
                any(),
                any(),
                any(),
                any()
        )).willReturn(0L);

        boolean disconnected = repository.markDisconnectedIfCurrentSession(
                1L,
                2L,
                "session-1",
                Instant.parse("2026-06-28T01:00:00Z"),
                Instant.parse("2026-06-28T01:01:00Z")
        );

        assertThat(disconnected).isFalse();
    }

    @Test
    void markDisconnectedIfCurrentSession_returnsTrue_whenRedisScriptReturnsPositiveGeneration() {
        given(redisTemplate.execute(
                any(RedisScript.class),
                anyList(),
                any(),
                any(),
                any(),
                any()
        )).willReturn(3L);

        boolean disconnected = repository.markDisconnectedIfCurrentSession(
                1L,
                2L,
                "session-1",
                Instant.parse("2026-06-28T01:00:00Z"),
                Instant.parse("2026-06-28T01:01:00Z")
        );

        assertThat(disconnected).isTrue();
    }

    @Test
    void findExpiredCandidates_returnsEmpty_whenRedisReturnsNull() {
        Instant now = Instant.parse("2026-06-28T01:00:00Z");
        given(zSetOperations.rangeByScore(EXPIRATIONS_KEY, 0, now.toEpochMilli(), 0, 10))
                .willReturn(null);

        List<RoomPresenceCandidate> candidates = repository.findExpiredCandidates(now, 10);

        assertThat(candidates).isEmpty();
    }

    @Test
    void findExpiredCandidates_returnsEmpty_whenRedisReturnsEmptySet() {
        Instant now = Instant.parse("2026-06-28T01:00:00Z");
        given(zSetOperations.rangeByScore(EXPIRATIONS_KEY, 0, now.toEpochMilli(), 0, 10))
                .willReturn(Set.of());

        List<RoomPresenceCandidate> candidates = repository.findExpiredCandidates(now, 10);

        assertThat(candidates).isEmpty();
    }

    @Test
    void findExpiredCandidates_usesMinimumLimitAndParsesMembers() {
        Instant now = Instant.parse("2026-06-28T01:00:00Z");
        given(zSetOperations.rangeByScore(EXPIRATIONS_KEY, 0, now.toEpochMilli(), 0, 1))
                .willReturn(Set.of("1:2:3"));

        List<RoomPresenceCandidate> candidates = repository.findExpiredCandidates(now, 0);

        assertThat(candidates).containsExactly(new RoomPresenceCandidate(1L, 2L, 3L));
    }

    @Test
    void isExpiredDisconnected_returnsFalse_whenRedisFieldsAreNull() {
        RoomPresenceCandidate candidate = new RoomPresenceCandidate(1L, 2L, 3L);
        given(hashOperations.multiGet(PRESENCE_KEY, List.of("status", "generation", "expiresAt")))
                .willReturn(null);

        boolean expired = repository.isExpiredDisconnected(
                candidate,
                Instant.parse("2026-06-28T01:00:00Z")
        );

        assertThat(expired).isFalse();
    }

    @Test
    void isExpiredDisconnected_returnsFalse_whenRedisFieldSizeIsInvalid() {
        RoomPresenceCandidate candidate = new RoomPresenceCandidate(1L, 2L, 3L);
        given(hashOperations.multiGet(PRESENCE_KEY, List.of("status", "generation", "expiresAt")))
                .willReturn(List.of("DISCONNECTED", "3"));

        boolean expired = repository.isExpiredDisconnected(
                candidate,
                Instant.parse("2026-06-28T01:00:00Z")
        );

        assertThat(expired).isFalse();
    }

    @Test
    void isExpiredDisconnected_returnsFalse_whenStatusIsConnected() {
        RoomPresenceCandidate candidate = new RoomPresenceCandidate(1L, 2L, 3L);
        given(hashOperations.multiGet(PRESENCE_KEY, List.of("status", "generation", "expiresAt")))
                .willReturn(List.of("CONNECTED", "3", "1782608399000"));

        boolean expired = repository.isExpiredDisconnected(
                candidate,
                Instant.parse("2026-06-28T01:00:00Z")
        );

        assertThat(expired).isFalse();
    }

    @Test
    void isExpiredDisconnected_returnsFalse_whenGenerationDoesNotMatch() {
        RoomPresenceCandidate candidate = new RoomPresenceCandidate(1L, 2L, 3L);
        given(hashOperations.multiGet(PRESENCE_KEY, List.of("status", "generation", "expiresAt")))
                .willReturn(List.of("DISCONNECTED", "4", "1782608399000"));

        boolean expired = repository.isExpiredDisconnected(
                candidate,
                Instant.parse("2026-06-28T01:00:00Z")
        );

        assertThat(expired).isFalse();
    }

    @Test
    void isExpiredDisconnected_returnsFalse_whenGenerationIsInvalid() {
        RoomPresenceCandidate candidate = new RoomPresenceCandidate(1L, 2L, 3L);
        given(hashOperations.multiGet(PRESENCE_KEY, List.of("status", "generation", "expiresAt")))
                .willReturn(List.of("DISCONNECTED", "invalid", "1782608399000"));

        boolean expired = repository.isExpiredDisconnected(
                candidate,
                Instant.parse("2026-06-28T01:00:00Z")
        );

        assertThat(expired).isFalse();
    }

    @Test
    void isExpiredDisconnected_returnsFalse_whenExpiresAtIsBlank() {
        RoomPresenceCandidate candidate = new RoomPresenceCandidate(1L, 2L, 3L);
        given(hashOperations.multiGet(PRESENCE_KEY, List.of("status", "generation", "expiresAt")))
                .willReturn(List.of("DISCONNECTED", "3", " "));

        boolean expired = repository.isExpiredDisconnected(
                candidate,
                Instant.parse("2026-06-28T01:00:00Z")
        );

        assertThat(expired).isFalse();
    }

    @Test
    void isExpiredDisconnected_returnsFalse_whenExpiresAtIsFuture() {
        RoomPresenceCandidate candidate = new RoomPresenceCandidate(1L, 2L, 3L);
        given(hashOperations.multiGet(PRESENCE_KEY, List.of("status", "generation", "expiresAt")))
                .willReturn(List.of("DISCONNECTED", "3", "1782608461000"));

        boolean expired = repository.isExpiredDisconnected(
                candidate,
                Instant.parse("2026-06-28T01:00:00Z")
        );

        assertThat(expired).isFalse();
    }

    @Test
    void isExpiredDisconnected_returnsTrue_whenDisconnectedGenerationMatchesAndExpired() {
        RoomPresenceCandidate candidate = new RoomPresenceCandidate(1L, 2L, 3L);
        given(hashOperations.multiGet(PRESENCE_KEY, List.of("status", "generation", "expiresAt")))
                .willReturn(List.of("DISCONNECTED", "3", "1782608399000"));

        boolean expired = repository.isExpiredDisconnected(
                candidate,
                Instant.parse("2026-06-28T01:00:00Z")
        );

        assertThat(expired).isTrue();
    }

    @Test
    void removeExpirationCandidate_removesCandidateMember() {
        RoomPresenceCandidate candidate = new RoomPresenceCandidate(1L, 2L, 3L);

        repository.removeExpirationCandidate(candidate);

        verify(zSetOperations).remove(EXPIRATIONS_KEY, "1:2:3");
    }

    @Test
    void incrementExpirationFailure_returnsZero_whenRedisReturnsNull() {
        RoomPresenceCandidate candidate = new RoomPresenceCandidate(1L, 2L, 3L);
        given(hashOperations.increment(EXPIRATION_FAILURES_KEY, "1:2:3", 1L))
                .willReturn(null);

        long count = repository.incrementExpirationFailure(candidate);

        assertThat(count).isZero();
    }

    @Test
    void incrementExpirationFailure_returnsCount_whenRedisReturnsValue() {
        RoomPresenceCandidate candidate = new RoomPresenceCandidate(1L, 2L, 3L);
        given(hashOperations.increment(EXPIRATION_FAILURES_KEY, "1:2:3", 1L))
                .willReturn(2L);

        long count = repository.incrementExpirationFailure(candidate);

        assertThat(count).isEqualTo(2L);
    }

    @Test
    void removeExpirationFailure_deletesCandidateMember() {
        RoomPresenceCandidate candidate = new RoomPresenceCandidate(1L, 2L, 3L);

        repository.removeExpirationFailure(candidate);

        verify(hashOperations).delete(EXPIRATION_FAILURES_KEY, "1:2:3");
    }

    @Test
    void deletePresence_deletesRoomUserPresenceKey() {
        repository.deletePresence(1L, 2L);

        verify(redisTemplate).delete(PRESENCE_KEY);
    }
}
