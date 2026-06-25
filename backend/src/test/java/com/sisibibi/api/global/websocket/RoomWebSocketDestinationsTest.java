package com.sisibibi.api.global.websocket;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RoomWebSocketDestinationsTest {

    @Test
    void roomTopics_followWebSocketContract() {
        assertThat(RoomWebSocketDestinations.chatEvents(1L))
                .isEqualTo("/topic/rooms/1/chat/events");
        assertThat(RoomWebSocketDestinations.stageEvents(1L))
                .isEqualTo("/topic/rooms/1/stage/events");
        assertThat(RoomWebSocketDestinations.participantEvents(1L))
                .isEqualTo("/topic/rooms/1/participants/events");
        assertThat(RoomWebSocketDestinations.roomEvents(1L))
                .isEqualTo("/topic/rooms/1/room/events");
        assertThat(RoomWebSocketDestinations.speechReactionEvents(1L))
                .isEqualTo("/topic/rooms/1/speech-reactions/events");
        assertThat(RoomWebSocketDestinations.speechEvents(1L))
                .isEqualTo("/topic/rooms/1/speeches/events");
        assertThat(RoomWebSocketDestinations.aiCounterIssueEvents(1L))
                .isEqualTo("/topic/rooms/1/ai-counter-issues/events");
        assertThat(RoomWebSocketDestinations.stageSummaryEvents(1L))
                .isEqualTo("/topic/rooms/1/stage-summary/events");
    }

    @Test
    void findAllowedRoomTopicId_returnsRoomId_forWhitelistedRoomTopics() {
        assertThat(RoomWebSocketDestinations.findAllowedRoomTopicId(
                "/topic/rooms/7/chat/events"
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
        assertThat(RoomWebSocketDestinations.findAllowedRoomTopicId(
                "/topic/rooms/7/speech-reactions/events"
        )).contains(7L);
        assertThat(RoomWebSocketDestinations.findAllowedRoomTopicId(
                "/topic/rooms/7/speeches/events"
        )).contains(7L);
        assertThat(RoomWebSocketDestinations.findAllowedRoomTopicId(
                "/topic/rooms/7/ai-counter-issues/events"
        )).contains(7L);
        assertThat(RoomWebSocketDestinations.findAllowedRoomTopicId(
                "/topic/rooms/7/stage-summary/events"
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
