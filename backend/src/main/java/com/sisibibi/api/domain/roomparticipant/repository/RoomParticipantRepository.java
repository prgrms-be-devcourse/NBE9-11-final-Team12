package com.sisibibi.api.domain.roomparticipant.repository;

import com.sisibibi.api.domain.roomparticipant.entity.RoomParticipant;
import com.sisibibi.api.domain.roomparticipant.entity.RoomParticipantStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomParticipantRepository extends JpaRepository<RoomParticipant, Long> {

    boolean existsByRoomIdAndUserIdAndStatus(
            Long roomId,
            Long userId,
            RoomParticipantStatus status
    );
}
