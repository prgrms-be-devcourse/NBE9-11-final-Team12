package com.sisibibi.api.domain.speech.dto.response;

import com.sisibibi.api.domain.speech.entity.Speech;
import com.sisibibi.api.domain.speech.entity.SpeechDeleteReason;
import com.sisibibi.api.domain.speech.entity.SpeechStance;
import com.sisibibi.api.domain.speech.entity.SpeechStatus;

import java.time.LocalDateTime;

public record SpeechListRes(
        Long speechId,
        Long roomId,
        Long userId,
        String content,
        SpeechStance stance,
        SpeechStance speakingStance,
        SpeechStatus status,
        String imageUrl,
        boolean deleted,
        SpeechDeleteReason deleteReason,
        LocalDateTime createdAt,
        long reactionCount,
        boolean reactedByMe
) {

    private static final String OFF_TOPIC_DELETED_CONTENT = "논점 이탈로 삭제된 의견입니다.";
    private static final String DELETED_CONTENT = "삭제된 의견입니다.";

    public SpeechListRes(
            Long speechId,
            Long roomId,
            Long userId,
            String content,
            SpeechStance stance,
            SpeechStatus status,
            String imageUrl,
            LocalDateTime createdAt,
            long reactionCount,
            boolean reactedByMe
    ) {
        this(
                speechId,
                roomId,
                userId,
                content,
                stance,
                stance,
                status,
                imageUrl,
                false,
                null,
                createdAt,
                reactionCount,
                reactedByMe
        );
    }

    public static SpeechListRes from(
            Speech speech,
            long reactionCount,
            boolean reactedByMe
    ) {
        return new SpeechListRes(
                speech.getId(),
                speech.getRoomId(),
                speech.getUserId(),
                displayContent(speech),
                speech.getStance(),
                speech.getSpeakingStance(),
                speech.getStatus(),
                speech.isDeleted() ? null : speech.getImageUrl(),
                speech.isDeleted(),
                speech.getDeleteReason(),
                speech.getCreatedAt(),
                reactionCount,
                reactedByMe
        );
    }

    private static String displayContent(Speech speech) {
        if (!speech.isDeleted()) {
            return speech.getContent();
        }
        if (speech.getDeleteReason() == SpeechDeleteReason.OFF_TOPIC) {
            return OFF_TOPIC_DELETED_CONTENT;
        }
        return DELETED_CONTENT;
    }
}
