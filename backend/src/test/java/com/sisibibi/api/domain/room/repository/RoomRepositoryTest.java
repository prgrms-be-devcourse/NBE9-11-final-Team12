package com.sisibibi.api.domain.room.repository;

import com.sisibibi.api.domain.room.entity.Room;
import com.sisibibi.api.global.config.JpaAuditingConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@Import(JpaAuditingConfig.class)
class RoomRepositoryTest {

    @Autowired
    private RoomRepository roomRepository;

    @Test
    void save_assignsCreatedAtByJpaAuditing() {
        Room room = Room.open(1L, "토론방");

        assertThat(room.getCreatedAt()).isNull();
        assertThat(room.getStartedAt()).isNotNull();

        Room saved = roomRepository.saveAndFlush(room);

        assertThat(saved.getCreatedAt()).isNotNull();
    }
}
