package com.sisibibi.api.domain.roomparticipant.repository.projection;

public interface RoomParticipantCountProjection {

    Long getRoomId();

    int getParticipantCount();
}
