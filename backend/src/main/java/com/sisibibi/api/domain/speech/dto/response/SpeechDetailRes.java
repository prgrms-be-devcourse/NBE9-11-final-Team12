package com.sisibibi.api.domain.speech.dto.response;

import com.sisibibi.api.domain.speech.entity.Speech;
import com.sisibibi.api.domain.speech.entity.SpeechDeleteReason;
import com.sisibibi.api.domain.speech.entity.SpeechStance;
import com.sisibibi.api.domain.speech.entity.SpeechStatus;

import java.time.LocalDateTime;

public record SpeechDetailRes(
        Long speechId,
        Long roomId,
        Long userId,
        Long speakingQueueId,
        String content,
        SpeechStance stance,
        SpeechStance speakingStance,
        String linkUrl,
        String imageUrl,
        SpeechStatus status,
        LocalDateTime startedAt,
        LocalDateTime endedAt,
        boolean deleted,
        SpeechDeleteReason deleteReason,
        LocalDateTime deletedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        long reactionCount,
        boolean reactedByMe
) {

    public SpeechDetailRes(
            Long speechId,
            Long roomId,
            Long userId,
            Long speakingQueueId,
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
        this(
                speechId,
                roomId,
                userId,
                speakingQueueId,
                content,
                stance,
                stance,
                linkUrl,
                imageUrl,
                status,
                startedAt,
                endedAt,
                false,
                null,
                null,
                createdAt,
                updatedAt,
                reactionCount,
                reactedByMe
        );
    }

    public static SpeechDetailRes from(
            Speech speech,
            long reactionCount,
            boolean reactedByMe
    ) {
        return new SpeechDetailRes(
                speech.getId(),
                speech.getRoomId(),
                speech.getUserId(),
                speech.getSpeakingQueueId(),
                speech.getContent(),
                speech.getStance(),
                speech.getSpeakingStance(),
                speech.getLinkUrl(),
                speech.getImageUrl(),
                speech.getStatus(),
                speech.getStartedAt(),
                speech.getEndedAt(),
                speech.isDeleted(),
                speech.getDeleteReason(),
                speech.getDeletedAt(),
                speech.getCreatedAt(),
                speech.getUpdatedAt(),
                reactionCount,
                reactedByMe
        );
    }
}
