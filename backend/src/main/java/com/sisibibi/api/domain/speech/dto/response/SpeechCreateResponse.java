package com.sisibibi.api.domain.speech.dto.response;

import com.sisibibi.api.domain.speech.entity.Speech;
import com.sisibibi.api.domain.speech.entity.SpeechStance;
import com.sisibibi.api.domain.speech.entity.SpeechStatus;

public record SpeechCreateResponse(
        Long speechId,
        Long roomId,
        Long userId,
        String content,
        SpeechStance stance,
        SpeechStatus status
) {

    public static SpeechCreateResponse from(Speech speech) {
        return new SpeechCreateResponse(
                speech.getId(),
                speech.getRoomId(),
                speech.getUserId(),
                speech.getContent(),
                speech.getStance(),
                speech.getStatus()
        );
    }
}
