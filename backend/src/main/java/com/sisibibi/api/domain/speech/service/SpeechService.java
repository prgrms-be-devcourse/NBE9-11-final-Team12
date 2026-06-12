package com.sisibibi.api.domain.speech.service;

import com.sisibibi.api.domain.room.entity.Room;
import com.sisibibi.api.domain.room.entity.RoomStatus;
import com.sisibibi.api.domain.room.repository.RoomRepository;
import com.sisibibi.api.domain.roomparticipant.entity.RoomParticipantStatus;
import com.sisibibi.api.domain.roomparticipant.repository.RoomParticipantRepository;
import com.sisibibi.api.domain.speech.dto.SpeechCreateCommand;
import com.sisibibi.api.domain.speech.dto.SpeechCreateResponse;
import com.sisibibi.api.domain.speech.entity.Speech;
import com.sisibibi.api.domain.speech.repository.SpeechRepository;
import com.sisibibi.api.global.exception.CustomException;
import com.sisibibi.api.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SpeechService {

    private final RoomRepository roomRepository;
    private final RoomParticipantRepository roomParticipantRepository;
    private final SpeechRepository speechRepository;

    @Transactional
    public SpeechCreateResponse createMainOpinion(
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

        return SpeechCreateResponse.from(speechRepository.save(speech));
    }
}
