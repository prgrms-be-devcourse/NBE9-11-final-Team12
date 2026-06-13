package com.sisibibi.api.domain.roomparticipant.repository;

import com.sisibibi.api.domain.roomparticipant.entity.RoomParticipant;
import com.sisibibi.api.domain.roomparticipant.entity.RoomParticipantStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoomParticipantRepository extends JpaRepository<RoomParticipant, Long> {

    boolean existsByRoomIdAndUserIdAndStatus(
            Long roomId,
            Long userId,
            RoomParticipantStatus status
    );

    Optional<RoomParticipant> findByRoomIdAndUserId(Long roomId, Long userId);
}
