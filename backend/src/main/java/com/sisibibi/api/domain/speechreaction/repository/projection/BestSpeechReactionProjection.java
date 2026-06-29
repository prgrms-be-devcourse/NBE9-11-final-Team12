package com.sisibibi.api.domain.speechreaction.repository.projection;

import com.sisibibi.api.domain.speech.entity.SpeechStance;
import com.sisibibi.api.domain.speech.entity.SpeechStatus;

import java.time.LocalDateTime;

public interface BestSpeechReactionProjection {

    Long getSpeechId();

    Long getRoomId();

    Long getUserId();

    String getContent();

    SpeechStance getStance();

    SpeechStatus getStatus();

    LocalDateTime getCreatedAt();

    long getReactionCount();
}
