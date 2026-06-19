package com.sisibibi.api.domain.speechreaction.service;

import com.sisibibi.api.domain.speech.repository.SpeechRepository;
import com.sisibibi.api.domain.speechreaction.dto.response.SpeechReactionCreateRes;
import com.sisibibi.api.domain.speechreaction.entity.SpeechReaction;
import com.sisibibi.api.domain.speechreaction.repository.SpeechReactionRepository;
import com.sisibibi.api.global.exception.CustomException;
import com.sisibibi.api.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SpeechReactionService {

    private final SpeechRepository speechRepository;
    private final SpeechReactionRepository speechReactionRepository;

    @Transactional
    public SpeechReactionCreateRes createReaction(Long speechId, Long userId) {
        validateSpeechExists(speechId);

        if (speechReactionRepository.existsBySpeechIdAndUserId(speechId, userId)) {
            throw new CustomException(ErrorCode.SPEECH_REACTION_ALREADY_EXISTS);
        }

        SpeechReaction reaction = SpeechReaction.create(speechId, userId);
        return SpeechReactionCreateRes.from(speechReactionRepository.save(reaction));
    }

    @Transactional
    public void deleteReaction(Long speechId, Long userId) {
        validateSpeechExists(speechId);

        SpeechReaction reaction = speechReactionRepository
                .findBySpeechIdAndUserId(speechId, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.SPEECH_REACTION_NOT_FOUND));

        speechReactionRepository.delete(reaction);
    }

    private void validateSpeechExists(Long speechId) {
        if (!speechRepository.existsByIdAndDeletedFalse(speechId)) {
            throw new CustomException(ErrorCode.SPEECH_NOT_FOUND);
        }
    }
}
