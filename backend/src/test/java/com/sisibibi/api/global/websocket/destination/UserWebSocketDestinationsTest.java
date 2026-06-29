package com.sisibibi.api.global.websocket.destination;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserWebSocketDestinationsTest {

    @Test
    void sanctionEvents_returnsUserSpecificTopic() {
        assertThat(UserWebSocketDestinations.sanctionEvents(10L))
                .isEqualTo("/topic/users/10/sanctions/events");
    }

    @Test
    void findSanctionEventUserId_returnsUserIdOnlyForAllowedTopic() {
        assertThat(UserWebSocketDestinations.findSanctionEventUserId(
                "/topic/users/10/sanctions/events"
        )).contains(10L);
        assertThat(UserWebSocketDestinations.findSanctionEventUserId(
                "/topic/users/10/private"
        )).isEmpty();
    }
}
