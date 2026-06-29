package com.sisibibi.api.global.websocket.presence;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;

class RoomPresenceServiceTest {

    private RedisRoomPresenceRepository roomPresenceRepository;
    private RoomPresenceProperties roomPresenceProperties;
    private RoomPresenceService roomPresenceService;

    @BeforeEach
    void setUp() {
        roomPresenceRepository = mock(RedisRoomPresenceRepository.class);
        roomPresenceProperties = new RoomPresenceProperties();
        roomPresenceProperties.setEnabled(true);
        roomPresenceProperties.setDisconnectGracePeriod(Duration.ofSeconds(60));
        roomPresenceService = new RoomPresenceService(
                roomPresenceRepository,
                roomPresenceProperties
        );
    }

    @Test
    void recordConnectedAt_marksRedisPresenceConnected() {
        Instant now = Instant.parse("2026-06-28T01:00:00Z");

        roomPresenceService.recordConnectedAt(1L, 2L, "session-1", now);

        verify(roomPresenceRepository).markConnected(1L, 2L, "session-1", now);
    }

    @Test
    void recordDisconnectedAt_marksRedisPresenceDisconnectedWithGracePeriod() {
        Instant now = Instant.parse("2026-06-28T01:00:00Z");

        roomPresenceService.recordDisconnectedAt(1L, 2L, "session-1", now);

        verify(roomPresenceRepository).markDisconnectedIfCurrentSession(
                1L,
                2L,
                "session-1",
                now,
                now.plus(Duration.ofSeconds(60))
        );
    }

    @Test
    void recordConnectedAt_doesNothingWhenPresenceIsDisabled() {
        roomPresenceProperties.setEnabled(false);

        roomPresenceService.recordConnectedAt(
                1L,
                2L,
                "session-1",
                Instant.parse("2026-06-28T01:00:00Z")
        );

        verifyNoInteractions(roomPresenceRepository);
    }

    @Test
    void recordConnectedAt_doesNotThrowWhenRedisFails() {
        Instant now = Instant.parse("2026-06-28T01:00:00Z");
        when(roomPresenceRepository.markConnected(1L, 2L, "session-1", now))
                .thenThrow(new IllegalStateException("redis unavailable"));

        roomPresenceService.recordConnectedAt(1L, 2L, "session-1", now);

        verify(roomPresenceRepository).markConnected(1L, 2L, "session-1", now);
    }

    @Test
    void clearPresence_deletesRoomUserPresence() {
        roomPresenceService.clearPresence(1L, 2L);

        verify(roomPresenceRepository).deletePresence(1L, 2L);
    }

    @Test
    void clearRoomPresence_deletesRoomPresence() {
        roomPresenceService.clearRoomPresence(1L);

        verify(roomPresenceRepository).deleteRoomPresence(1L);
    }

    @Test
    void cleanupExpiredDisconnectedPresenceAt_usesRetentionAndBatchSize() {
        roomPresenceProperties.setCleanupRetention(Duration.ofMinutes(10));
        roomPresenceProperties.setCleanupBatchSize(25);
        Instant now = Instant.parse("2026-06-28T01:10:00Z");

        roomPresenceService.cleanupExpiredDisconnectedPresenceAt(now);

        verify(roomPresenceRepository).cleanupExpiredDisconnectedPresence(
                Instant.parse("2026-06-28T01:00:00Z"),
                25
        );
    }
}
