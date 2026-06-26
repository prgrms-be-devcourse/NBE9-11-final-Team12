package com.sisibibi.api.domain.speech.repository;

import com.sisibibi.api.domain.speech.entity.Speech;
import com.sisibibi.api.domain.speech.entity.SpeechStance;
import com.sisibibi.api.domain.speech.entity.SpeechStatus;
import com.sisibibi.api.global.config.JpaAuditingConfig;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@Import(JpaAuditingConfig.class)
class SpeechRepositoryTest {

    @Autowired
    private SpeechRepository speechRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void saveAndUpdate_applyJpaAuditingTimestamps() {
        Speech speech = Speech.createMainOpinion(1L, 10L, "의견", SpeechStance.PRO);

        assertThat(speech.getCreatedAt()).isNull();
        assertThat(speech.getUpdatedAt()).isNull();

        Speech saved = speechRepository.saveAndFlush(speech);

        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();

        LocalDateTime createdAt = saved.getCreatedAt();
        saved.updateMainOpinion("수정된 의견", SpeechStance.CON);
        Speech updated = speechRepository.saveAndFlush(saved);

        assertThat(updated.getCreatedAt()).isEqualTo(createdAt);
        assertThat(updated.getUpdatedAt()).isAfterOrEqualTo(createdAt);
    }

    @Test
    void findByRoomIdBeforeCursor_returnsIdsBeforeCursorInDescendingOrder() {
        Speech older = Speech.createMainOpinion(1L, 10L, "이전 의견", SpeechStance.CON);
        Speech latestFirst = Speech.createMainOpinion(1L, 20L, "최신 의견 1", SpeechStance.PRO);
        Speech latestSecond = Speech.createMainOpinion(1L, 30L, "최신 의견 2", SpeechStance.CON);
        Speech otherRoom = Speech.createMainOpinion(2L, 30L, "다른 방 의견", SpeechStance.PRO);
        ReflectionTestUtils.setField(older, "createdAt", LocalDateTime.of(2026, 6, 12, 10, 0));
        ReflectionTestUtils.setField(latestFirst, "createdAt", LocalDateTime.of(2026, 6, 12, 11, 0));
        ReflectionTestUtils.setField(latestSecond, "createdAt", LocalDateTime.of(2026, 6, 12, 11, 0));
        ReflectionTestUtils.setField(otherRoom, "createdAt", LocalDateTime.of(2026, 6, 12, 12, 0));
        speechRepository.saveAllAndFlush(List.of(older, latestFirst, latestSecond, otherRoom));

        Long cursor = latestSecond.getId();
        List<Speech> speeches = speechRepository.findByRoomIdBeforeCursor(
                1L,
                cursor,
                PageRequest.of(0, 2)
        );

        assertThat(speeches).extracting(Speech::getContent)
                .containsExactly("최신 의견 1", "이전 의견");
    }

    @Test
    void findByRoomIdBeforeCursor_returnsFirstPage_whenCursorIsNull() {
        Speech first = Speech.createMainOpinion(1L, 10L, "첫 의견", SpeechStance.CON);
        Speech second = Speech.createMainOpinion(1L, 20L, "두 번째 의견", SpeechStance.PRO);
        speechRepository.saveAllAndFlush(List.of(first, second));

        List<Speech> speeches = speechRepository.findByRoomIdBeforeCursor(
                1L,
                null,
                PageRequest.of(0, 1)
        );

        assertThat(speeches).extracting(Speech::getContent)
                .containsExactly("두 번째 의견");
    }

    @Test
    void findByRoomIdBeforeCursor_excludesSoftDeletedSpeeches() {
        Speech visible = Speech.createMainOpinion(1L, 10L, "보이는 의견", SpeechStance.CON);
        Speech deleted = Speech.createMainOpinion(1L, 20L, "삭제된 의견", SpeechStance.PRO);
        deleted.softDelete(LocalDateTime.of(2026, 6, 12, 12, 0));
        speechRepository.saveAllAndFlush(List.of(visible, deleted));

        List<Speech> speeches = speechRepository.findByRoomIdBeforeCursor(
                1L,
                null,
                PageRequest.of(0, 10)
        );

        assertThat(speeches).extracting(Speech::getContent)
                .containsExactly("보이는 의견");
    }

    @Test
    void countByUserIdAndDeletedFalse_countsVisibleAuthoredSpeeches() {
        Speech first = Speech.createMainOpinion(1L, 10L, "첫 의견", SpeechStance.PRO);
        Speech second = Speech.createMainOpinion(2L, 10L, "두 번째 의견", SpeechStance.CON);
        Speech deleted = Speech.createMainOpinion(1L, 10L, "삭제 의견", SpeechStance.PRO);
        Speech otherUser = Speech.createMainOpinion(1L, 20L, "다른 사용자 의견", SpeechStance.CON);
        deleted.softDelete(LocalDateTime.of(2026, 6, 23, 12, 0));
        speechRepository.saveAllAndFlush(List.of(first, second, deleted, otherUser));

        long count = speechRepository.countByUserIdAndDeletedFalse(10L);

        assertThat(count).isEqualTo(2L);
    }

    @Test
    void completeSpeakingSpeeches_completesOnlyActiveVisibleSpeechesForRoomAndUser() {
        LocalDateTime endedAt = LocalDateTime.of(2026, 6, 24, 12, 0);
        Speech target = Speech.createMainOpinion(1L, 10L, "진행 중 의견", SpeechStance.PRO);
        Speech alreadyCompleted =
                Speech.createMainOpinion(1L, 10L, "이미 완료된 의견", SpeechStance.CON);
        Speech otherUser = Speech.createMainOpinion(1L, 20L, "다른 사용자 의견", SpeechStance.PRO);
        Speech otherRoom = Speech.createMainOpinion(2L, 10L, "다른 방 의견", SpeechStance.CON);
        Speech deleted = Speech.createMainOpinion(1L, 10L, "삭제된 의견", SpeechStance.PRO);
        ReflectionTestUtils.setField(alreadyCompleted, "status", SpeechStatus.COMPLETED);
        deleted.softDelete(LocalDateTime.of(2026, 6, 24, 11, 0));
        speechRepository.saveAllAndFlush(List.of(
                target,
                alreadyCompleted,
                otherUser,
                otherRoom,
                deleted
        ));

        int updatedCount = speechRepository.completeSpeakingSpeeches(
                1L,
                10L,
                SpeechStatus.SPEAKING,
                SpeechStatus.COMPLETED,
                endedAt
        );

        assertThat(updatedCount).isEqualTo(1);
        assertThat(speechRepository.findById(target.getId()).orElseThrow().getStatus())
                .isEqualTo(SpeechStatus.COMPLETED);
        assertThat(speechRepository.findById(target.getId()).orElseThrow().getEndedAt())
                .isEqualTo(endedAt);
        assertThat(speechRepository.findById(alreadyCompleted.getId()).orElseThrow().getEndedAt())
                .isNull();
        assertThat(speechRepository.findById(otherUser.getId()).orElseThrow().getStatus())
                .isEqualTo(SpeechStatus.SPEAKING);
        assertThat(speechRepository.findById(otherRoom.getId()).orElseThrow().getStatus())
                .isEqualTo(SpeechStatus.SPEAKING);
        assertThat(speechRepository.findById(deleted.getId()).orElseThrow().getStatus())
                .isEqualTo(SpeechStatus.SPEAKING);
    }

    @Test
    void findStageSummarySourceSpeeches_returnsVisibleNonBlankSpeechesUntilTriggeredAtInChronologicalOrder() {
        LocalDateTime triggeredAt = LocalDateTime.of(2026, 6, 26, 12, 30);
        Speech first = Speech.createMainOpinion(1L, 10L, "첫 번째 의견", SpeechStance.PRO);
        Speech secondSameTimeLowerId = Speech.createMainOpinion(1L, 20L, "두 번째 의견", SpeechStance.CON);
        Speech secondSameTimeHigherId = Speech.createMainOpinion(1L, 30L, "세 번째 의견", SpeechStance.PRO);
        Speech afterTrigger = Speech.createMainOpinion(1L, 40L, "늦은 의견", SpeechStance.CON);
        Speech blank = Speech.createMainOpinion(1L, 50L, "   \n  ", SpeechStance.PRO);
        Speech deleted = Speech.createMainOpinion(1L, 60L, "삭제된 의견", SpeechStance.CON);
        Speech otherRoom = Speech.createMainOpinion(2L, 70L, "다른 방 의견", SpeechStance.PRO);
        ReflectionTestUtils.setField(first, "createdAt", LocalDateTime.of(2026, 6, 26, 12, 0));
        ReflectionTestUtils.setField(secondSameTimeLowerId, "createdAt", LocalDateTime.of(2026, 6, 26, 12, 10));
        ReflectionTestUtils.setField(secondSameTimeHigherId, "createdAt", LocalDateTime.of(2026, 6, 26, 12, 10));
        ReflectionTestUtils.setField(afterTrigger, "createdAt", LocalDateTime.of(2026, 6, 26, 12, 31));
        ReflectionTestUtils.setField(blank, "createdAt", LocalDateTime.of(2026, 6, 26, 12, 15));
        ReflectionTestUtils.setField(deleted, "createdAt", LocalDateTime.of(2026, 6, 26, 12, 20));
        ReflectionTestUtils.setField(otherRoom, "createdAt", LocalDateTime.of(2026, 6, 26, 12, 5));
        deleted.softDelete(LocalDateTime.of(2026, 6, 26, 12, 21));
        List<Speech> saved = speechRepository.saveAllAndFlush(List.of(
                first,
                secondSameTimeLowerId,
                secondSameTimeHigherId,
                afterTrigger,
                blank,
                deleted,
                otherRoom
        ));
        updateCreatedAt(saved.get(0).getId(), LocalDateTime.of(2026, 6, 26, 12, 0));
        updateCreatedAt(saved.get(1).getId(), LocalDateTime.of(2026, 6, 26, 12, 10));
        updateCreatedAt(saved.get(2).getId(), LocalDateTime.of(2026, 6, 26, 12, 10));
        updateCreatedAt(saved.get(3).getId(), LocalDateTime.of(2026, 6, 26, 12, 31));
        updateCreatedAt(saved.get(4).getId(), LocalDateTime.of(2026, 6, 26, 12, 15));
        updateCreatedAt(saved.get(5).getId(), LocalDateTime.of(2026, 6, 26, 12, 20));
        updateCreatedAt(saved.get(6).getId(), LocalDateTime.of(2026, 6, 26, 12, 5));
        entityManager.flush();
        entityManager.clear();

        List<Speech> speeches = speechRepository.findStageSummarySourceSpeeches(1L, triggeredAt);

        assertThat(speeches)
                .extracting(Speech::getContent)
                .containsExactly("첫 번째 의견", "두 번째 의견", "세 번째 의견");
    }

    private void updateCreatedAt(Long speechId, LocalDateTime createdAt) {
        entityManager.createNativeQuery("""
                        update speeches
                        set created_at = :createdAt
                        where id = :speechId
                        """)
                .setParameter("createdAt", createdAt)
                .setParameter("speechId", speechId)
                .executeUpdate();
    }

}
