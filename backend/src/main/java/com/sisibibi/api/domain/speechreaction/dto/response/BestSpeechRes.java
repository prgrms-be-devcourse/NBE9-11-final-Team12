package com.sisibibi.api.domain.speechreaction.dto.response;

import com.sisibibi.api.domain.speech.entity.Speech;
import com.sisibibi.api.domain.speech.entity.SpeechStance;
import com.sisibibi.api.domain.speech.entity.SpeechStatus;
import com.sisibibi.api.domain.speechreaction.repository.projection.BestSpeechReactionProjection;

import java.time.LocalDateTime;

public record BestSpeechRes(
        Long speechId,
        Long roomId,
        Long userId,
        String content,
        SpeechStance stance,
        SpeechStatus status,
        LocalDateTime createdAt,
        long reactionCount
) {

    public static BestSpeechRes from(Speech speech, long reactionCount) {
        return new BestSpeechRes(
                speech.getId(),
                speech.getRoomId(),
                speech.getUserId(),
                speech.getContent(),
                speech.getStance(),
                speech.getStatus(),
                speech.getCreatedAt(),
                reactionCount
        );
    }

    public static BestSpeechRes from(BestSpeechReactionProjection projection) {
        return new BestSpeechRes(
                projection.getSpeechId(),
                projection.getRoomId(),
                projection.getUserId(),
                projection.getContent(),
                projection.getStance(),
                projection.getStatus(),
                projection.getCreatedAt(),
                projection.getReactionCount()
        );
    }
}
