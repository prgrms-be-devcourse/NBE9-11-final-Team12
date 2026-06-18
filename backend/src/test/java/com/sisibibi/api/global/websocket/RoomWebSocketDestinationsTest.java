package com.sisibibi.api.global.websocket;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RoomWebSocketDestinationsTest {

    @Test
    void roomTopics_followWebSocketContract() {
        assertThat(RoomWebSocketDestinations.chatMessages(1L))
                .isEqualTo("/topic/rooms/1/chat/messages");
        assertThat(RoomWebSocketDestinations.stageEvents(1L))
                .isEqualTo("/topic/rooms/1/stage/events");
        assertThat(RoomWebSocketDestinations.participantEvents(1L))
                .isEqualTo("/topic/rooms/1/participants/events");
        assertThat(RoomWebSocketDestinations.roomEvents(1L))
                .isEqualTo("/topic/rooms/1/room/events");
    }

    @Test
    void findAllowedRoomTopicId_returnsRoomId_forWhitelistedRoomTopics() {
        assertThat(RoomWebSocketDestinations.findAllowedRoomTopicId(
                "/topic/rooms/7/chat/messages"
        )).contains(7L);
        assertThat(RoomWebSocketDestinations.findAllowedRoomTopicId(
                "/topic/rooms/7/stage/events"
        )).contains(7L);
        assertThat(RoomWebSocketDestinations.findAllowedRoomTopicId(
                "/topic/rooms/7/participants/events"
        )).contains(7L);
        assertThat(RoomWebSocketDestinations.findAllowedRoomTopicId(
                "/topic/rooms/7/room/events"
        )).contains(7L);
    }

    @Test
    void findAllowedRoomTopicId_returnsEmpty_forUnknownRoomTopic() {
        assertThat(RoomWebSocketDestinations.findAllowedRoomTopicId(
                "/topic/rooms/7/unknown"
        )).isEmpty();
        assertThat(RoomWebSocketDestinations.isRoomTopic(
                "/topic/rooms/7/unknown"
        )).isTrue();
        assertThat(RoomWebSocketDestinations.isRoomTopic(
                "/topic/rooms/not-a-number/unknown"
        )).isTrue();
    }
}
