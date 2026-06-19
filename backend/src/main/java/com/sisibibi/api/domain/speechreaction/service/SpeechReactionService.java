package com.sisibibi.api.domain.speechreaction.service;

import com.sisibibi.api.domain.room.repository.RoomRepository;
import com.sisibibi.api.domain.roomparticipant.entity.RoomParticipantStatus;
import com.sisibibi.api.domain.roomparticipant.repository.RoomParticipantRepository;
import com.sisibibi.api.domain.speech.entity.Speech;
import com.sisibibi.api.domain.speech.repository.SpeechRepository;
import com.sisibibi.api.domain.speechreaction.dto.response.BestSpeechRes;
import com.sisibibi.api.domain.speechreaction.dto.response.SpeechReactionCreateRes;
import com.sisibibi.api.domain.speechreaction.entity.SpeechReaction;
import com.sisibibi.api.domain.speechreaction.repository.SpeechReactionRepository;
import com.sisibibi.api.domain.speechreaction.repository.projection.BestSpeechReactionProjection;
import com.sisibibi.api.global.exception.CustomException;
import com.sisibibi.api.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SpeechReactionService {

    private final RoomRepository roomRepository;
    private final RoomParticipantRepository roomParticipantRepository;
    private final SpeechRepository speechRepository;
    private final SpeechReactionRepository speechReactionRepository;

    @Transactional
    public SpeechReactionCreateRes createReaction(Long speechId, Long userId) {
        Speech speech = getActiveSpeech(speechId);
        validateJoinedParticipant(speech.getRoomId(), userId);

        if (speech.getUserId().equals(userId)) {
            throw new CustomException(ErrorCode.SPEECH_REACTION_SELF_NOT_ALLOWED);
        }

        if (speechReactionRepository.existsBySpeechIdAndUserId(speechId, userId)) {
            throw new CustomException(ErrorCode.SPEECH_REACTION_ALREADY_EXISTS);
        }

        SpeechReaction reaction = SpeechReaction.create(speechId, userId);
        return SpeechReactionCreateRes.from(speechReactionRepository.saveAndFlush(reaction));
    }

    @Transactional
    public void deleteReaction(Long speechId, Long userId) {
        getActiveSpeech(speechId);

        SpeechReaction reaction = speechReactionRepository
                .findBySpeechIdAndUserId(speechId, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.SPEECH_REACTION_NOT_FOUND));

        speechReactionRepository.delete(reaction);
    }

    @Transactional(readOnly = true)
    public BestSpeechRes getBestSpeech(Long roomId) {
        if (!roomRepository.existsById(roomId)) {
            throw new CustomException(ErrorCode.ROOM_NOT_FOUND);
        }

        List<BestSpeechReactionProjection> results =
                speechReactionRepository.findBestSpeechReactions(
                        roomId,
                        PageRequest.of(0, 1)
                );

        if (results.isEmpty()) {
            throw new CustomException(ErrorCode.BEST_SPEECH_NOT_FOUND);
        }

        BestSpeechReactionProjection bestReaction = results.getFirst();
        Speech speech = speechRepository.findByIdAndDeletedFalse(bestReaction.getSpeechId())
                .orElseThrow(() -> new CustomException(ErrorCode.BEST_SPEECH_NOT_FOUND));

        return BestSpeechRes.from(speech, bestReaction.getReactionCount());
    }

    private Speech getActiveSpeech(Long speechId) {
        return speechRepository.findByIdAndDeletedFalse(speechId)
                .orElseThrow(() -> new CustomException(ErrorCode.SPEECH_NOT_FOUND));
    }

    private void validateJoinedParticipant(Long roomId, Long userId) {
        if (!roomParticipantRepository.existsByRoomIdAndUserIdAndStatus(
                roomId,
                userId,
                RoomParticipantStatus.JOINED
        )) {
            throw new CustomException(ErrorCode.ROOM_PARTICIPATION_REQUIRED);
        }
    }
}
