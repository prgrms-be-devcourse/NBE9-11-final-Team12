package com.sisibibi.api.domain.speech.repository.projection;

import java.time.LocalDateTime;

public interface CurrentSpeakerProjection {

    Long getUserId();

    String getNickname();

    Integer getQueueOrder();

    LocalDateTime getAssignedAt();

    LocalDateTime getExpiresAt();
}
