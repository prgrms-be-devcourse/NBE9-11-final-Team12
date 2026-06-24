package com.sisibibi.api.domain.speech.dto.event;

import com.sisibibi.api.domain.speech.entity.Speech;
import java.time.LocalDateTime;

public record SpeechEventPayload(
        Long roomId,
        Long speechId,
        Long userId,
        LocalDateTime occurredAt
) {

    public static SpeechEventPayload from(Speech speech) {
        return new SpeechEventPayload(
                speech.getRoomId(),
                speech.getId(),
                speech.getUserId(),
                LocalDateTime.now()
        );
    }
}
