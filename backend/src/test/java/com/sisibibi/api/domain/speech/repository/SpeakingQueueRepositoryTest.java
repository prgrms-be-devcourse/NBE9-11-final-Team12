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
        SpeakingQueue request = SpeakingQueue.waiting(
                1L,
                7L,
                LocalDateTime.of(2026, 6, 12, 11, 30)
        );
        SpeakingQueue saved = speakingQueueRepository.saveAndFlush(request);
        saved.assignQueueOrderFromId();
        speakingQueueRepository.flush();

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getQueueOrder()).isEqualTo(Math.toIntExact(saved.getId()));
        assertThat(speakingQueueRepository.existsByRoomIdAndUserIdAndStatusIn(
                1L,
                7L,
                List.of(SpeakingQueueStatus.WAITING, SpeakingQueueStatus.ASSIGNED)
        )).isTrue();
    }

    @Test
    void save_rejectsDuplicateActiveRequestInSameRoom() {
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
                        7L,
                        2,
                        LocalDateTime.of(2026, 6, 12, 11, 31)
                )
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void findFirstWaiting_returnsLowestQueueOrder() {
        speakingQueueRepository.saveAndFlush(
                SpeakingQueue.waiting(
                        1L,
                        10L,
                        20,
                        LocalDateTime.of(2026, 6, 12, 11, 31)
                )
        );
        SpeakingQueue first = speakingQueueRepository.saveAndFlush(
                SpeakingQueue.waiting(
                        1L,
                        20L,
                        15,
                        LocalDateTime.of(2026, 6, 12, 11, 30)
                )
        );

        SpeakingQueue found = speakingQueueRepository
                .findFirstByRoomIdAndStatusOrderByQueueOrderAsc(
                        1L,
                        SpeakingQueueStatus.WAITING
                )
                .orElseThrow();

        assertThat(found.getId()).isEqualTo(first.getId());
        assertThat(found.getQueueOrder()).isEqualTo(15);
    }
}
