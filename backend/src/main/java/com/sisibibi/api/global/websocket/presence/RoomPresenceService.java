package com.sisibibi.api.global.websocket.presence;

import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoomPresenceService {

    private final RedisRoomPresenceRepository roomPresenceRepository;
    private final RoomPresenceProperties roomPresenceProperties;

    public void recordConnected(Long roomId, Long userId, String sessionId) {
        recordConnectedAt(roomId, userId, sessionId, Instant.now());
    }

    void recordConnectedAt(Long roomId, Long userId, String sessionId, Instant connectedAt) {
        if (!roomPresenceProperties.isEnabled()) {
            return;
        }

        try {
            roomPresenceRepository.markConnected(roomId, userId, sessionId, connectedAt);
        } catch (RuntimeException redisException) {
            log.warn(
                    "Failed to record room WebSocket presence connection. "
                            + "roomId={}, userId={}, sessionId={}",
                    roomId,
                    userId,
                    sessionId,
                    redisException
            );
        }
    }

    public void recordDisconnected(Long roomId, Long userId, String sessionId) {
        recordDisconnectedAt(roomId, userId, sessionId, Instant.now());
    }

    void recordDisconnectedAt(Long roomId, Long userId, String sessionId, Instant disconnectedAt) {
        if (!roomPresenceProperties.isEnabled()) {
            return;
        }

        try {
            roomPresenceRepository.markDisconnectedIfCurrentSession(
                    roomId,
                    userId,
                    sessionId,
                    disconnectedAt,
                    disconnectedAt.plus(roomPresenceProperties.getDisconnectGracePeriod())
            );
        } catch (RuntimeException redisException) {
            log.warn(
                    "Failed to record room WebSocket presence disconnection. "
                            + "roomId={}, userId={}, sessionId={}",
                    roomId,
                    userId,
                    sessionId,
                    redisException
            );
        }
    }

    public void clearPresence(Long roomId, Long userId) {
        if (!roomPresenceProperties.isEnabled()) {
            return;
        }

        try {
            roomPresenceRepository.deletePresence(roomId, userId);
        } catch (RuntimeException redisException) {
            log.warn(
                    "Failed to clear room WebSocket presence. roomId={}, userId={}",
                    roomId,
                    userId,
                    redisException
            );
        }
    }

    public void clearRoomPresence(Long roomId) {
        if (!roomPresenceProperties.isEnabled()) {
            return;
        }

        try {
            long deletedCount = roomPresenceRepository.deleteRoomPresence(roomId);
            if (deletedCount > 0) {
                log.info("Cleared room WebSocket presence. roomId={}, deletedCount={}",
                        roomId,
                        deletedCount);
            }
        } catch (RuntimeException redisException) {
            log.warn(
                    "Failed to clear room WebSocket presence. roomId={}",
                    roomId,
                    redisException
            );
        }
    }

    public void cleanupExpiredDisconnectedPresence() {
        cleanupExpiredDisconnectedPresenceAt(Instant.now());
    }

    void cleanupExpiredDisconnectedPresenceAt(Instant now) {
        if (!roomPresenceProperties.isEnabled()) {
            return;
        }

        try {
            Instant cutoff = now.minus(roomPresenceProperties.getCleanupRetention());
            long cleanedCount = roomPresenceRepository.cleanupExpiredDisconnectedPresence(
                    cutoff,
                    roomPresenceProperties.getCleanupBatchSize()
            );
            if (cleanedCount > 0) {
                log.info("Cleaned expired disconnected room presence. cleanedCount={}",
                        cleanedCount);
            }
        } catch (RuntimeException redisException) {
            log.warn("Failed to clean expired disconnected room presence.", redisException);
        }
    }
}
