package com.sisibibi.api.global.websocket.presence;

import com.sisibibi.api.domain.roomparticipant.service.RoomParticipantService;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoomPresenceExpirationService {

    private final RedisRoomPresenceRepository roomPresenceRepository;
    private final RoomPresenceProperties roomPresenceProperties;
    private final RoomParticipantService roomParticipantService;

    public void expireDisconnectedParticipants() {
        expireDisconnectedParticipantsAt(Instant.now());
    }

    void expireDisconnectedParticipantsAt(Instant now) {
        if (!roomPresenceProperties.isEnabled()) {
            return;
        }

        for (RoomPresenceCandidate candidate : findExpiredCandidates(now)) {
            expireCandidate(candidate, now);
        }
    }

    private List<RoomPresenceCandidate> findExpiredCandidates(Instant now) {
        try {
            return roomPresenceRepository.findExpiredCandidates(
                    now,
                    roomPresenceProperties.getExpirationBatchSize()
            );
        } catch (RuntimeException redisException) {
            log.warn("Failed to find expired room presence candidates.", redisException);
            return List.of();
        }
    }

    private void expireCandidate(RoomPresenceCandidate candidate, Instant now) {
        try {
            if (!roomPresenceRepository.isExpiredDisconnected(candidate, now)) {
                removeCandidate(candidate);
                return;
            }

            roomParticipantService.leaveRoom(candidate.roomId(), candidate.userId());
            removeCandidate(candidate);
            log.info(
                    "Room participant left after WebSocket disconnect grace period. "
                            + "roomId={}, userId={}, generation={}",
                    candidate.roomId(),
                    candidate.userId(),
                    candidate.generation()
            );
        } catch (RuntimeException expirationException) {
            long failureCount = incrementExpirationFailure(candidate);
            if (failureCount >= roomPresenceProperties.getMaxExpirationFailures()) {
                removeCandidate(candidate);
                log.error(
                        "Removed failed room presence expiration candidate after max failures. "
                                + "roomId={}, userId={}, generation={}, failureCount={}",
                        candidate.roomId(),
                        candidate.userId(),
                        candidate.generation(),
                        failureCount,
                        expirationException
                );
                return;
            }

            log.error(
                    "Failed to expire disconnected room presence. Will retry later. "
                            + "roomId={}, userId={}, generation={}, failureCount={}",
                    candidate.roomId(),
                    candidate.userId(),
                    candidate.generation(),
                    failureCount,
                    expirationException
            );
        }
    }

    private long incrementExpirationFailure(RoomPresenceCandidate candidate) {
        try {
            return roomPresenceRepository.incrementExpirationFailure(candidate);
        } catch (RuntimeException redisException) {
            log.error(
                    "Failed to record room presence expiration failure. "
                            + "roomId={}, userId={}, generation={}",
                    candidate.roomId(),
                    candidate.userId(),
                    candidate.generation(),
                    redisException
            );
            return 0L;
        }
    }

    private void removeCandidate(RoomPresenceCandidate candidate) {
        roomPresenceRepository.removeExpirationCandidate(candidate);
        roomPresenceRepository.removeExpirationFailure(candidate);
    }
}
