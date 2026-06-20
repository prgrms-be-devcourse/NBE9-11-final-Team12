package com.sisibibi.api.domain.speechreaction.dto.event;

import java.time.LocalDateTime;

public record SpeechReactionEventPayload(
        Long roomId,
        Long speechId,
        long reactionCount,
        LocalDateTime occurredAt
) {

    public static SpeechReactionEventPayload of(
            Long roomId,
            Long speechId,
            long reactionCount
    ) {
        return new SpeechReactionEventPayload(
                roomId,
                speechId,
                reactionCount,
                LocalDateTime.now()
        );
    }
}
