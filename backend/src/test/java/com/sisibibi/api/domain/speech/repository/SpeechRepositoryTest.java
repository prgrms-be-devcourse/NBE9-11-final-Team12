package com.sisibibi.api.domain.speech.repository;

import com.sisibibi.api.domain.speech.entity.Speech;
import com.sisibibi.api.domain.speech.entity.SpeechStance;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class SpeechRepositoryTest {

    @Autowired
    private SpeechRepository speechRepository;

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
}
