package com.sisibibi.api.domain.speech.dto.response;

import com.sisibibi.api.domain.speech.entity.Speech;
import com.sisibibi.api.domain.speech.entity.SpeechStance;
import com.sisibibi.api.domain.speech.entity.SpeechStatus;

import java.time.LocalDateTime;

public record SpeechListRes(
        Long speechId,
        Long roomId,
        Long userId,
        String content,
        SpeechStance stance,
        SpeechStatus status,
        LocalDateTime createdAt,
        long reactionCount,
        boolean reactedByMe
) {

    public static SpeechListRes from(
            Speech speech,
            long reactionCount,
            boolean reactedByMe
    ) {
        return new SpeechListRes(
                speech.getId(),
                speech.getRoomId(),
                speech.getUserId(),
                speech.getContent(),
                speech.getStance(),
                speech.getStatus(),
                speech.getCreatedAt(),
                reactionCount,
                reactedByMe
        );
    }
}
