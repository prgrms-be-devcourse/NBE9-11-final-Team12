package com.sisibibi.api.domain.speech.dto.response;

import com.sisibibi.api.domain.speech.entity.Speech;
import com.sisibibi.api.domain.speech.entity.SpeechStance;
import com.sisibibi.api.domain.speech.entity.SpeechStatus;

import java.time.LocalDateTime;

public record SpeechDetailRes(
        Long speechId,
        Long roomId,
        Long userId,
        String content,
        SpeechStance stance,
        String linkUrl,
        String imageUrl,
        SpeechStatus status,
        LocalDateTime startedAt,
        LocalDateTime endedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        long reactionCount,
        boolean reactedByMe
) {

    public static SpeechDetailRes from(
            Speech speech,
            long reactionCount,
            boolean reactedByMe
    ) {
        return new SpeechDetailRes(
                speech.getId(),
                speech.getRoomId(),
                speech.getUserId(),
                speech.getContent(),
                speech.getStance(),
                speech.getLinkUrl(),
                speech.getImageUrl(),
                speech.getStatus(),
                speech.getStartedAt(),
                speech.getEndedAt(),
                speech.getCreatedAt(),
                speech.getUpdatedAt(),
                reactionCount,
                reactedByMe
        );
    }
}
