package com.sisibibi.api.domain.speech.service;

import com.sisibibi.api.domain.room.entity.Room;
import com.sisibibi.api.domain.room.entity.RoomStatus;
import com.sisibibi.api.domain.room.repository.RoomRepository;
import com.sisibibi.api.domain.roomparticipant.entity.RoomParticipantStatus;
import com.sisibibi.api.domain.roomparticipant.repository.RoomParticipantRepository;
import com.sisibibi.api.domain.speech.dto.command.SpeechCreateCommand;
import com.sisibibi.api.domain.speech.dto.command.SpeechUpdateCommand;
import com.sisibibi.api.domain.speech.dto.response.SpeechCreateRes;
import com.sisibibi.api.domain.speech.dto.response.SpeechCursorPageRes;
import com.sisibibi.api.domain.speech.dto.response.SpeechDetailRes;
import com.sisibibi.api.domain.speech.dto.response.SpeechListRes;
import com.sisibibi.api.domain.speech.entity.Speech;
import com.sisibibi.api.domain.speech.entity.SpeechStatus;
import com.sisibibi.api.domain.speech.repository.SpeechRepository;
import com.sisibibi.api.domain.speechreaction.repository.SpeechReactionRepository;
import com.sisibibi.api.domain.speechreaction.repository.projection.SpeechReactionSummaryProjection;
import com.sisibibi.api.global.exception.CustomException;
import com.sisibibi.api.global.exception.ErrorCode;
import com.sisibibi.api.global.moderation.ProfanityDetector;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SpeechService {

    private final RoomRepository roomRepository;
    private final RoomParticipantRepository roomParticipantRepository;
    private final SpeechRepository speechRepository;
    private final SpeechReactionRepository speechReactionRepository;
    private final ProfanityDetector profanityDetector;

    @Transactional
    public SpeechCreateRes createMainOpinion(
            Long roomId,
            Long userId,
            SpeechCreateCommand command
    ) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new CustomException(ErrorCode.ROOM_NOT_FOUND));

        if (room.getStatus() != RoomStatus.OPEN) {
            throw new CustomException(ErrorCode.ROOM_CLOSED);
        }

        boolean isParticipating = roomParticipantRepository.existsByRoomIdAndUserIdAndStatus(
                roomId,
                userId,
                RoomParticipantStatus.JOINED
        );

        if (!isParticipating) {
            throw new CustomException(ErrorCode.ROOM_PARTICIPATION_REQUIRED);
        }

        validateContent(command.content(), "create", roomId, userId, null);

        Speech speech = Speech.createMainOpinion(
                roomId,
                userId,
                command.content(),
                command.stance()
        );

        return SpeechCreateRes.from(speechRepository.save(speech));
    }

    @Transactional(readOnly = true)
    public SpeechCursorPageRes getSpeeches(Long roomId, Long userId, Long cursor, int size) {
        if (!roomRepository.existsById(roomId)) {
            throw new CustomException(ErrorCode.ROOM_NOT_FOUND);
        }

        List<Speech> speeches = speechRepository.findByRoomIdBeforeCursor(
                roomId,
                cursor,
                PageRequest.of(0, size + 1)
        );
        boolean hasNext = speeches.size() > size;
        List<Speech> pageSpeeches = speeches.stream()
                .limit(size)
                .toList();
        Map<Long, SpeechReactionSummaryProjection> reactionSummaries =
                getReactionSummaries(pageSpeeches, userId);
        List<SpeechListRes> items = pageSpeeches.stream()
                .map(speech -> {
                    SpeechReactionSummaryProjection summary =
                            reactionSummaries.get(speech.getId());
                    return SpeechListRes.from(
                            speech,
                            summary == null ? 0 : summary.getReactionCount(),
                            summary != null && summary.getMyReactionCount() > 0
                    );
                })
                .toList();
        Long nextCursor = hasNext ? items.get(items.size() - 1).speechId() : null;

        return new SpeechCursorPageRes(items, nextCursor, hasNext);
    }

    @Transactional(readOnly = true)
    public SpeechDetailRes getSpeech(Long speechId, Long userId) {
        Speech speech = speechRepository.findByIdAndDeletedFalse(speechId)
                .orElseThrow(() -> new CustomException(ErrorCode.SPEECH_NOT_FOUND));

        return toDetailResponse(speech, userId);
    }

    @Transactional
    public SpeechDetailRes updateSpeech(
            Long speechId,
            Long userId,
            SpeechUpdateCommand command
    ) {
        Speech speech = findEditableOwnedSpeech(speechId, userId);

        validateContent(command.content(), "update", speech.getRoomId(), userId, speechId);
        speech.updateMainOpinion(command.content(), command.stance());
        return toDetailResponse(speech, userId);
    }

    @Transactional
    public void deleteSpeech(Long speechId, Long userId) {
        Speech speech = findEditableOwnedSpeech(speechId, userId);

        speech.softDelete(LocalDateTime.now());
        log.info(
                "Speech soft deleted. speechId={}, roomId={}, userId={}",
                speechId,
                speech.getRoomId(),
                userId
        );
    }

    @Transactional
    public SpeechDetailRes updateSpeechLink(Long speechId, Long userId, String linkUrl) {
        Speech speech = findEditableOwnedSpeech(speechId, userId);

        speech.updateLink(linkUrl);
        return toDetailResponse(speech, userId);
    }

    private Speech findEditableOwnedSpeech(Long speechId, Long userId) {
        Speech speech = speechRepository.findByIdAndDeletedFalse(speechId)
                .orElseThrow(() -> new CustomException(ErrorCode.SPEECH_NOT_FOUND));

        if (!speech.getUserId().equals(userId)) {
            throw new CustomException(ErrorCode.SPEECH_ACCESS_DENIED);
        }

        if (speech.getStatus() == SpeechStatus.COMPLETED) {
            throw new CustomException(ErrorCode.SPEECH_NOT_EDITABLE);
        }

        return speech;
    }

    private void validateContent(
            String content,
            String action,
            Long roomId,
            Long userId,
            Long speechId
    ) {
        if (profanityDetector.containsProfanity(content)) {
            if (speechId == null) {
                log.warn(
                        "Speech content blocked by profanity detector. action={}, roomId={}, userId={}",
                        action,
                        roomId,
                        userId
                );
            } else {
                log.warn(
                        "Speech content blocked by profanity detector. "
                                + "action={}, roomId={}, userId={}, speechId={}",
                        action,
                        roomId,
                        userId,
                        speechId
                );
            }
            throw new CustomException(ErrorCode.SPEECH_CONTENT_CONTAINS_PROFANITY);
        }
    }

    private Map<Long, SpeechReactionSummaryProjection> getReactionSummaries(
            List<Speech> speeches,
            Long userId
    ) {
        if (speeches.isEmpty()) {
            return Collections.emptyMap();
        }

        List<Long> speechIds = speeches.stream()
                .map(Speech::getId)
                .toList();

        return speechReactionRepository.findReactionSummaries(speechIds, userId).stream()
                .collect(Collectors.toMap(
                        SpeechReactionSummaryProjection::getSpeechId,
                        Function.identity()
                ));
    }

    private SpeechDetailRes toDetailResponse(Speech speech, Long userId) {
        SpeechReactionSummaryProjection summary =
                getReactionSummaries(List.of(speech), userId).get(speech.getId());
        return SpeechDetailRes.from(
                speech,
                summary == null ? 0 : summary.getReactionCount(),
                summary != null && summary.getMyReactionCount() > 0
        );
    }
}
