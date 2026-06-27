package com.sisibibi.api.domain.roomparticipant.service;

import com.sisibibi.api.domain.roomparticipant.dto.event.RoomParticipantChangedEvent;
import com.sisibibi.api.domain.roomparticipant.dto.event.RoomParticipantEventPayload;
import com.sisibibi.api.domain.roomparticipant.dto.event.RoomParticipantEventType;
import com.sisibibi.api.domain.roomparticipant.entity.RoomParticipant;
import com.sisibibi.api.domain.roomparticipant.entity.RoomParticipantStatus;
import com.sisibibi.api.domain.roomparticipant.repository.RoomParticipantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoomParticipantForceLeaveService {

    private final RoomParticipantRepository roomParticipantRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public int leaveAllJoinedRooms(Long userId) {
        List<RoomParticipant> participants = roomParticipantRepository
                .findByUserIdAndStatus(userId, RoomParticipantStatus.JOINED);

        for (RoomParticipant participant : participants) {
            participant.leave();
            publishParticipantLeftEvent(participant);
        }

        if (!participants.isEmpty()) {
            log.info(
                    "User force-left joined rooms. userId={}, roomCount={}",
                    userId,
                    participants.size()
            );
        }
        return participants.size();
    }

    private void publishParticipantLeftEvent(RoomParticipant participant) {
        int participantCount = roomParticipantRepository.countByRoomIdAndStatus(
                participant.getRoomId(),
                RoomParticipantStatus.JOINED
        );
        eventPublisher.publishEvent(new RoomParticipantChangedEvent(
                RoomParticipantEventType.PARTICIPANT_LEFT,
                participant.getRoomId(),
                RoomParticipantEventPayload.of(
                        participant.getRoomId(),
                        participant.getUserId(),
                        participantCount
                )
        ));
    }
}
