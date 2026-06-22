package com.sisibibi.api.domain.speech.repository.projection;

import com.sisibibi.api.domain.speech.entity.SpeechStance;
import java.time.LocalDateTime;

public interface CurrentSpeakerProjection {

    Long getUserId();

    String getNickname();

    SpeechStance getStance();

    Integer getQueueOrder();

    LocalDateTime getAssignedAt();

    LocalDateTime getExpiresAt();
}
