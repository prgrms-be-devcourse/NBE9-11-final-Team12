package com.sisibibi.api.domain.roomparticipant.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.sisibibi.api.domain.room.entity.Room;
import com.sisibibi.api.domain.room.repository.RoomRepository;
import com.sisibibi.api.domain.roomparticipant.entity.RoomParticipant;
import com.sisibibi.api.domain.roomparticipant.entity.RoomParticipantStatus;
import com.sisibibi.api.domain.roomparticipant.repository.projection.RoomParticipantCountProjection;
import com.sisibibi.api.global.config.JpaAuditingConfig;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@Import(JpaAuditingConfig.class)
class RoomParticipantRepositoryTest {

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private RoomParticipantRepository roomParticipantRepository;

    @Test
    void findParticipantCount_returnsZero_whenRoomExistsWithoutJoinedParticipant() {
        Room room = roomRepository.saveAndFlush(Room.open(
                1L,
                "참여자 없는 토론방",
                LocalDateTime.of(2026, 6, 29, 10, 0),
                LocalDateTime.of(2026, 6, 29, 12, 0),
                100
        ));

        Optional<RoomParticipantCountProjection> result =
                roomParticipantRepository.findParticipantCount(room.getId(), RoomParticipantStatus.JOINED);

        assertThat(result).isPresent();
        assertThat(result.get().getRoomId()).isEqualTo(room.getId());
        assertThat(result.get().getParticipantCount()).isZero();
    }

    @Test
    void findParticipantCount_countsOnlyJoinedParticipants() {
        Room room = roomRepository.saveAndFlush(Room.open(
                2L,
                "참여자 있는 토론방",
                LocalDateTime.of(2026, 6, 29, 10, 0),
                LocalDateTime.of(2026, 6, 29, 12, 0),
                100
        ));
        RoomParticipant joined = roomParticipantRepository.save(RoomParticipant.join(room.getId(), 1L));
        RoomParticipant left = RoomParticipant.join(room.getId(), 2L);
        left.leave();
        roomParticipantRepository.saveAndFlush(left);

        Optional<RoomParticipantCountProjection> result =
                roomParticipantRepository.findParticipantCount(room.getId(), RoomParticipantStatus.JOINED);

        assertThat(result).isPresent();
        assertThat(result.get().getRoomId()).isEqualTo(room.getId());
        assertThat(result.get().getParticipantCount()).isEqualTo(1);
        assertThat(joined.getStatus()).isEqualTo(RoomParticipantStatus.JOINED);
    }

    @Test
    void findParticipantCount_returnsEmpty_whenRoomDoesNotExist() {
        Optional<RoomParticipantCountProjection> result =
                roomParticipantRepository.findParticipantCount(999L, RoomParticipantStatus.JOINED);

        assertThat(result).isEmpty();
    }
}
