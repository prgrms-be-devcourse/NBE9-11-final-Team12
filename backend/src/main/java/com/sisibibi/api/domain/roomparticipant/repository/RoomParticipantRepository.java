package com.sisibibi.api.domain.roomparticipant.repository;

import com.sisibibi.api.domain.roomparticipant.entity.RoomParticipant;
import com.sisibibi.api.domain.roomparticipant.entity.RoomParticipantStatus;
import com.sisibibi.api.domain.roomparticipant.repository.projection.RoomParticipantCountProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RoomParticipantRepository extends JpaRepository<RoomParticipant, Long> {

    boolean existsByRoomIdAndUserIdAndStatus(
            Long roomId,
            Long userId,
            RoomParticipantStatus status
    );

    Optional<RoomParticipant> findByRoomIdAndUserId(Long roomId, Long userId);

    List<RoomParticipant> findByRoomIdAndStatusOrderByJoinedAtAsc(
        Long roomId,
        RoomParticipantStatus status
    );

    List<RoomParticipant> findByUserIdAndStatus(
            Long userId,
            RoomParticipantStatus status
    );

    int countByRoomId(Long roomId);

    int countByRoomIdAndStatus(Long roomId, RoomParticipantStatus status);

    @Query("""
            select room.id as roomId,
                   count(participant.id) as participantCount
            from Room room
            left join RoomParticipant participant
              on participant.roomId = room.id
             and participant.status = :status
            where room.id = :roomId
            group by room.id
            """)
    Optional<RoomParticipantCountProjection> findParticipantCount(
            @Param("roomId") Long roomId,
            @Param("status") RoomParticipantStatus status
    );

    long countByUserId(Long userId);
}
