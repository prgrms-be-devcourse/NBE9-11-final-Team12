package com.sisibibi.api.domain.speech.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.sisibibi.api.domain.speech.entity.RoomQueueSequence;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
class RoomQueueSequenceRepositoryTest {

    @Autowired
    private RoomQueueSequenceRepository roomQueueSequenceRepository;

    @Test
    void findByRoomIdForUpdate_returnsSequence() {
        RoomQueueSequence queueSequence = roomQueueSequenceRepository.saveAndFlush(
                RoomQueueSequence.create(1L, LocalDateTime.of(2026, 6, 24, 15, 0))
        );

        RoomQueueSequence found = roomQueueSequenceRepository
                .findByRoomIdForUpdate(1L)
                .orElseThrow();

        assertThat(found).isSameAs(queueSequence);
        assertThat(found.getNextQueueOrder()).isEqualTo(1);
    }
}
