package com.sisibibi.api.domain.speech.repository.projection;

public interface SpeakingRequestEligibilityProjection {

    Integer getRoomExists();

    Integer getRoomActive();

    Integer getJoinedParticipant();

    Integer getActiveRequestExists();

    Integer getRestricted();
}
