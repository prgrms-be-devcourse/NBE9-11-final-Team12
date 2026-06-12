package com.sisibibi.api.domain.speech.service;

import com.sisibibi.api.domain.room.entity.Room;
import com.sisibibi.api.domain.room.entity.RoomStatus;
import com.sisibibi.api.domain.room.repository.RoomRepository;
import com.sisibibi.api.domain.roomparticipant.entity.RoomParticipantStatus;
import com.sisibibi.api.domain.roomparticipant.repository.RoomParticipantRepository;
import com.sisibibi.api.domain.speech.dto.request.SpeechCreateCommand;
import com.sisibibi.api.domain.speech.dto.response.SpeechCreateRes;
import com.sisibibi.api.domain.speech.dto.response.SpeechCursorPageRes;
import com.sisibibi.api.domain.speech.dto.response.SpeechDetailRes;
import com.sisibibi.api.domain.speech.dto.response.SpeechListRes;
import com.sisibibi.api.domain.speech.entity.Speech;
import com.sisibibi.api.domain.speech.repository.SpeechRepository;
import com.sisibibi.api.global.exception.CustomException;
import com.sisibibi.api.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.PageRequest;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SpeechService {

    private final RoomRepository roomRepository;
    private final RoomParticipantRepository roomParticipantRepository;
    private final SpeechRepository speechRepository;

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

        Speech speech = Speech.createMainOpinion(
                roomId,
                userId,
                command.content(),
                command.stance()
        );

        return SpeechCreateRes.from(speechRepository.save(speech));
    }

    @Transactional(readOnly = true)
    public SpeechCursorPageRes getSpeeches(Long roomId, Long cursor, int size) {
        if (!roomRepository.existsById(roomId)) {
            throw new CustomException(ErrorCode.ROOM_NOT_FOUND);
        }

        List<Speech> speeches = speechRepository.findByRoomIdBeforeCursor(
                roomId,
                cursor,
                PageRequest.of(0, size + 1)
        );
        boolean hasNext = speeches.size() > size;
        List<SpeechListRes> items = speeches.stream()
                .limit(size)
                .map(SpeechListRes::from)
                .toList();
        Long nextCursor = hasNext ? items.get(items.size() - 1).speechId() : null;

        return new SpeechCursorPageRes(items, nextCursor, hasNext);
    }

    @Transactional(readOnly = true)
    public SpeechDetailRes getSpeech(Long speechId) {
        Speech speech = speechRepository.findById(speechId)
                .orElseThrow(() -> new CustomException(ErrorCode.SPEECH_NOT_FOUND));

        return SpeechDetailRes.from(speech);
    }
}
