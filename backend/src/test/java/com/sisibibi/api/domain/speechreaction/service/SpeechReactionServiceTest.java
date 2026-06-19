package com.sisibibi.api.domain.speechreaction.service;

import com.sisibibi.api.domain.speech.repository.SpeechRepository;
import com.sisibibi.api.domain.speechreaction.dto.response.SpeechReactionCreateRes;
import com.sisibibi.api.domain.speechreaction.entity.SpeechReaction;
import com.sisibibi.api.domain.speechreaction.repository.SpeechReactionRepository;
import com.sisibibi.api.global.exception.CustomException;
import com.sisibibi.api.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SpeechReactionServiceTest {

    @Mock
    private SpeechRepository speechRepository;

    @Mock
    private SpeechReactionRepository speechReactionRepository;

    @InjectMocks
    private SpeechReactionService speechReactionService;

    @Test
    void createReaction_savesReaction_whenSpeechExistsAndUserHasNotReacted() {
        given(speechRepository.existsByIdAndDeletedFalse(10L)).willReturn(true);
        given(speechReactionRepository.existsBySpeechIdAndUserId(10L, 20L))
                .willReturn(false);
        given(speechReactionRepository.save(org.mockito.ArgumentMatchers.any()))
                .willAnswer(invocation -> {
                    SpeechReaction reaction = invocation.getArgument(0);
                    ReflectionTestUtils.setField(reaction, "id", 100L);
                    ReflectionTestUtils.setField(
                            reaction,
                            "createdAt",
                            LocalDateTime.of(2026, 6, 19, 12, 0)
                    );
                    return reaction;
                });

        SpeechReactionCreateRes response = speechReactionService.createReaction(10L, 20L);

        ArgumentCaptor<SpeechReaction> captor = ArgumentCaptor.forClass(SpeechReaction.class);
        verify(speechReactionRepository).save(captor.capture());
        assertThat(captor.getValue().getSpeechId()).isEqualTo(10L);
        assertThat(captor.getValue().getUserId()).isEqualTo(20L);
        assertThat(response.reactionId()).isEqualTo(100L);
        assertThat(response.createdAt()).isEqualTo(LocalDateTime.of(2026, 6, 19, 12, 0));
    }

    @Test
    void createReaction_throwsSpeechNotFound_whenSpeechDoesNotExistOrIsDeleted() {
        given(speechRepository.existsByIdAndDeletedFalse(10L)).willReturn(false);

        assertThatThrownBy(() -> speechReactionService.createReaction(10L, 20L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.SPEECH_NOT_FOUND);

        verify(speechReactionRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void createReaction_throwsAlreadyExists_whenUserAlreadyReacted() {
        given(speechRepository.existsByIdAndDeletedFalse(10L)).willReturn(true);
        given(speechReactionRepository.existsBySpeechIdAndUserId(10L, 20L))
                .willReturn(true);

        assertThatThrownBy(() -> speechReactionService.createReaction(10L, 20L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.SPEECH_REACTION_ALREADY_EXISTS);

        verify(speechReactionRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void deleteReaction_deletesExistingReaction() {
        SpeechReaction reaction = SpeechReaction.create(10L, 20L);
        given(speechRepository.existsByIdAndDeletedFalse(10L)).willReturn(true);
        given(speechReactionRepository.findBySpeechIdAndUserId(10L, 20L))
                .willReturn(Optional.of(reaction));

        speechReactionService.deleteReaction(10L, 20L);

        verify(speechReactionRepository).delete(reaction);
    }

    @Test
    void deleteReaction_throwsSpeechNotFound_whenSpeechDoesNotExistOrIsDeleted() {
        given(speechRepository.existsByIdAndDeletedFalse(10L)).willReturn(false);

        assertThatThrownBy(() -> speechReactionService.deleteReaction(10L, 20L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.SPEECH_NOT_FOUND);

        verify(speechReactionRepository, never()).delete(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void deleteReaction_throwsReactionNotFound_whenUserHasNotReacted() {
        given(speechRepository.existsByIdAndDeletedFalse(10L)).willReturn(true);
        given(speechReactionRepository.findBySpeechIdAndUserId(10L, 20L))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> speechReactionService.deleteReaction(10L, 20L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.SPEECH_REACTION_NOT_FOUND);
    }
}
