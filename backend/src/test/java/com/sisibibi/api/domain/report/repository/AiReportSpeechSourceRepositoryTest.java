package com.sisibibi.api.domain.report.repository;

import com.sisibibi.api.domain.speech.entity.Speech;
import com.sisibibi.api.domain.speech.entity.SpeechStance;
import com.sisibibi.api.domain.speech.entity.SpeechStatus;
import com.sisibibi.api.domain.speech.repository.SpeechRepository;
import com.sisibibi.api.global.config.JpaAuditingConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@Import(JpaAuditingConfig.class)
class AiReportSpeechSourceRepositoryTest {

    @Autowired
    private SpeechRepository speechRepository;

    @Test
    void findAiReportSourceSpeeches_returnsOnlyCompletedNonDeletedValidSpeechesInCreatedOrder() {
        Speech first = completedSpeech(1L, 10L, "첫 번째 완료 발언", SpeechStance.PRO,
                LocalDateTime.of(2026, 6, 22, 10, 0));
        Speech second = completedSpeech(1L, 20L, "두 번째 완료 발언", SpeechStance.CON,
                LocalDateTime.of(2026, 6, 22, 10, 5));
        Speech ready = Speech.createMainOpinion(1L, 30L, "준비 중 발언", SpeechStance.PRO);
        Speech blank = completedSpeech(1L, 40L, "   ", SpeechStance.PRO,
                LocalDateTime.of(2026, 6, 22, 10, 1));
        Speech nullStance = completedSpeech(1L, 50L, "입장이 없는 발언", SpeechStance.CON,
                LocalDateTime.of(2026, 6, 22, 10, 2));
        Speech deleted = completedSpeech(1L, 60L, "삭제된 발언", SpeechStance.CON,
                LocalDateTime.of(2026, 6, 22, 10, 3));
        Speech otherRoom = completedSpeech(2L, 70L, "다른 방 발언", SpeechStance.PRO,
                LocalDateTime.of(2026, 6, 22, 10, 4));

        ReflectionTestUtils.setField(nullStance, "stance", null);
        deleted.softDelete(LocalDateTime.of(2026, 6, 22, 10, 4));
        speechRepository.saveAllAndFlush(List.of(
                first,
                ready,
                blank,
                nullStance,
                deleted,
                otherRoom,
                second
        ));

        List<Speech> result = speechRepository.findAiReportSourceSpeeches(1L);

        assertThat(result).extracting(Speech::getContent)
                .containsExactly("첫 번째 완료 발언", "두 번째 완료 발언");
    }

    private Speech completedSpeech(
            Long roomId,
            Long userId,
            String content,
            SpeechStance stance,
            LocalDateTime createdAt
    ) {
        Speech speech = Speech.createMainOpinion(roomId, userId, content, stance);
        ReflectionTestUtils.setField(speech, "status", SpeechStatus.COMPLETED);
        ReflectionTestUtils.setField(speech, "createdAt", createdAt);
        ReflectionTestUtils.setField(speech, "startedAt", createdAt.minusMinutes(1));
        ReflectionTestUtils.setField(speech, "endedAt", createdAt.plusMinutes(2));
        return speech;
    }
}
