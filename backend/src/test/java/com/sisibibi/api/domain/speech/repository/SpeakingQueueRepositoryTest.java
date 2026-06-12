package com.sisibibi.api.domain.speech.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sisibibi.api.domain.speech.entity.SpeakingQueue;
import com.sisibibi.api.domain.speech.entity.SpeakingQueueStatus;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
class SpeakingQueueRepositoryTest {

    @Autowired
    private SpeakingQueueRepository speakingQueueRepository;

    @Test
    void save_persistsWaitingRequest() {
        SpeakingQueue saved = speakingQueueRepository.saveAndFlush(
                SpeakingQueue.waiting(
                        1L,
                        7L,
                        1,
                        LocalDateTime.of(2026, 6, 12, 11, 30)
                )
        );

        assertThat(saved.getId()).isNotNull();
        assertThat(speakingQueueRepository.existsByRoomIdAndUserIdAndStatusIn(
                1L,
                7L,
                List.of(SpeakingQueueStatus.WAITING, SpeakingQueueStatus.ASSIGNED)
        )).isTrue();
    }

    @Test
    void save_rejectsDuplicateQueueOrderInSameRoom() {
        speakingQueueRepository.saveAndFlush(
                SpeakingQueue.waiting(
                        1L,
                        7L,
                        1,
                        LocalDateTime.of(2026, 6, 12, 11, 30)
                )
        );

        assertThatThrownBy(() -> speakingQueueRepository.saveAndFlush(
                SpeakingQueue.waiting(
                        1L,
                        8L,
                        1,
                        LocalDateTime.of(2026, 6, 12, 11, 31)
                )
        )).isInstanceOf(DataIntegrityViolationException.class);
    }
}
