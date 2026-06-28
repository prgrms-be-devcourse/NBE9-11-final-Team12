package com.sisibibi.api.global.websocket;

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
}
