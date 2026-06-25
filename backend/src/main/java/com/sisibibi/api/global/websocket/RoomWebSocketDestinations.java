package com.sisibibi.api.global.websocket;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class RoomWebSocketDestinations {

    private static final Pattern ALLOWED_ROOM_TOPIC_PATTERN = Pattern.compile(
            "^/topic/rooms/(\\d+)/"
                    + "(chat/events|stage/events|participants/events|room/events|"
                    + "speeches/events|speech-reactions/events|ai-counter-issues/events)$"
    );
    private static final Pattern ROOM_TOPIC_PATTERN = Pattern.compile("^/topic/rooms/[^/]+/.*$");

    private RoomWebSocketDestinations() {
    }

    public static String chatEvents(Long roomId) {
        return "/topic/rooms/" + roomId + "/chat/events";
    }

    public static String stageEvents(Long roomId) {
        return "/topic/rooms/" + roomId + "/stage/events";
    }

    public static String participantEvents(Long roomId) {
        return "/topic/rooms/" + roomId + "/participants/events";
    }

    public static String roomEvents(Long roomId) {
        return "/topic/rooms/" + roomId + "/room/events";
    }

    public static String speechReactionEvents(Long roomId) {
        return "/topic/rooms/" + roomId + "/speech-reactions/events";
    }

    public static String speechEvents(Long roomId) {
        return "/topic/rooms/" + roomId + "/speeches/events";
    }

    public static String aiCounterIssueEvents(Long roomId) {
        return "/topic/rooms/" + roomId + "/ai-counter-issues/events";
    }

    public static Optional<Long> findAllowedRoomTopicId(String destination) {
        if (destination == null) {
            return Optional.empty();
        }

        Matcher matcher = ALLOWED_ROOM_TOPIC_PATTERN.matcher(destination);
        if (!matcher.matches()) {
            return Optional.empty();
        }

        return Optional.of(Long.valueOf(matcher.group(1)));
    }

    public static boolean isRoomTopic(String destination) {
        return destination != null && ROOM_TOPIC_PATTERN.matcher(destination).matches();
    }
}
