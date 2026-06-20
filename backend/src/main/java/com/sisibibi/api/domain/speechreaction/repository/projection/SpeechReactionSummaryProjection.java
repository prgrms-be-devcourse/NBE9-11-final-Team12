package com.sisibibi.api.domain.speechreaction.repository.projection;

public interface SpeechReactionSummaryProjection {

    Long getSpeechId();

    long getReactionCount();

    long getMyReactionCount();
}
