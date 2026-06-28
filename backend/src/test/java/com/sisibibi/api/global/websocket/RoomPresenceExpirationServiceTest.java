package com.sisibibi.api.global.websocket;

import com.sisibibi.api.domain.roomparticipant.service.RoomParticipantService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class RoomPresenceExpirationServiceTest {

    private RedisRoomPresenceRepository roomPresenceRepository;
    private RoomPresenceProperties roomPresenceProperties;
    private RoomParticipantService roomParticipantService;
    private RoomPresenceExpirationService roomPresenceExpirationService;

    @BeforeEach
    void setUp() {
        roomPresenceRepository = mock(RedisRoomPresenceRepository.class);
        roomPresenceProperties = new RoomPresenceProperties();
        roomPresenceProperties.setEnabled(true);
        roomPresenceProperties.setExpirationBatchSize(10);
        roomParticipantService = mock(RoomParticipantService.class);
        roomPresenceExpirationService = new RoomPresenceExpirationService(
                roomPresenceRepository,
                roomPresenceProperties,
                roomParticipantService
        );
    }

    @Test
    void expireDisconnectedParticipantsAt_leavesExpiredDisconnectedParticipant() {
        Instant now = Instant.parse("2026-06-28T01:01:00Z");
        RoomPresenceCandidate candidate = new RoomPresenceCandidate(1L, 2L, 3L);
        given(roomPresenceRepository.findExpiredCandidates(now, 10))
                .willReturn(List.of(candidate));
        given(roomPresenceRepository.isExpiredDisconnected(candidate, now)).willReturn(true);

        roomPresenceExpirationService.expireDisconnectedParticipantsAt(now);

        verify(roomParticipantService).leaveRoom(1L, 2L);
        verify(roomPresenceRepository).removeExpirationCandidate(candidate);
    }

    @Test
    void expireDisconnectedParticipantsAt_removesStaleCandidateWithoutLeaving() {
        Instant now = Instant.parse("2026-06-28T01:01:00Z");
        RoomPresenceCandidate candidate = new RoomPresenceCandidate(1L, 2L, 3L);
        given(roomPresenceRepository.findExpiredCandidates(now, 10))
                .willReturn(List.of(candidate));
        given(roomPresenceRepository.isExpiredDisconnected(candidate, now)).willReturn(false);

        roomPresenceExpirationService.expireDisconnectedParticipantsAt(now);

        verify(roomParticipantService, never()).leaveRoom(1L, 2L);
        verify(roomPresenceRepository).removeExpirationCandidate(candidate);
    }

    @Test
    void expireDisconnectedParticipantsAt_continuesWhenOneCandidateFails() {
        Instant now = Instant.parse("2026-06-28T01:01:00Z");
        RoomPresenceCandidate first = new RoomPresenceCandidate(1L, 2L, 3L);
        RoomPresenceCandidate second = new RoomPresenceCandidate(1L, 3L, 4L);
        given(roomPresenceRepository.findExpiredCandidates(now, 10))
                .willReturn(List.of(first, second));
        given(roomPresenceRepository.isExpiredDisconnected(first, now)).willReturn(true);
        given(roomPresenceRepository.isExpiredDisconnected(second, now)).willReturn(true);
        willThrow(new IllegalStateException("database unavailable"))
                .given(roomParticipantService)
                .leaveRoom(1L, 2L);

        roomPresenceExpirationService.expireDisconnectedParticipantsAt(now);

        verify(roomParticipantService).leaveRoom(1L, 2L);
        verify(roomParticipantService).leaveRoom(1L, 3L);
        verify(roomPresenceRepository, never()).removeExpirationCandidate(first);
        verify(roomPresenceRepository).removeExpirationCandidate(second);
    }

    @Test
    void expireDisconnectedParticipantsAt_doesNothingWhenPresenceIsDisabled() {
        roomPresenceProperties.setEnabled(false);
        Instant now = Instant.parse("2026-06-28T01:01:00Z");

        roomPresenceExpirationService.expireDisconnectedParticipantsAt(now);

        verify(roomPresenceRepository, never())
                .findExpiredCandidates(
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.anyInt()
                );
    }
}
