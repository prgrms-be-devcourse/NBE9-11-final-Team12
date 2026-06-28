package com.sisibibi.api.global.websocket;

import com.sisibibi.api.domain.roomparticipant.service.RoomParticipantService;
import java.time.Instant;
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

        for (RoomPresenceCandidate candidate : roomPresenceRepository.findExpiredCandidates(
                now,
                roomPresenceProperties.getExpirationBatchSize()
        )) {
            expireCandidate(candidate, now);
        }
    }

    private void expireCandidate(RoomPresenceCandidate candidate, Instant now) {
        try {
            if (!roomPresenceRepository.isExpiredDisconnected(candidate, now)) {
                roomPresenceRepository.removeExpirationCandidate(candidate);
                return;
            }

            roomParticipantService.leaveRoom(candidate.roomId(), candidate.userId());
            roomPresenceRepository.removeExpirationCandidate(candidate);
            log.info(
                    "Room participant left after WebSocket disconnect grace period. "
                            + "roomId={}, userId={}, generation={}",
                    candidate.roomId(),
                    candidate.userId(),
                    candidate.generation()
            );
        } catch (RuntimeException expirationException) {
            log.error(
                    "Failed to expire disconnected room presence. "
                            + "roomId={}, userId={}, generation={}",
                    candidate.roomId(),
                    candidate.userId(),
                    candidate.generation(),
                    expirationException
            );
        }
    }
}
