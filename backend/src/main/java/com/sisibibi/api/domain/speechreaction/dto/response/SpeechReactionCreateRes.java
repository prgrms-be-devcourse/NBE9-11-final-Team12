package com.sisibibi.api.domain.speechreaction.dto.response;

import com.sisibibi.api.domain.speechreaction.entity.SpeechReaction;

import java.time.LocalDateTime;

public record SpeechReactionCreateRes(
        Long reactionId,
        Long speechId,
        Long userId,
        LocalDateTime createdAt
) {

    public static SpeechReactionCreateRes from(SpeechReaction reaction) {
        return new SpeechReactionCreateRes(
                reaction.getId(),
                reaction.getSpeechId(),
                reaction.getUserId(),
                reaction.getCreatedAt()
        );
    }
}
