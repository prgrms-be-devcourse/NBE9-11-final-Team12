package com.sisibibi.api.global.websocket.destination;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class UserWebSocketDestinations {

    private static final Pattern SANCTION_EVENT_PATTERN = Pattern.compile(
            "^/topic/users/(\\d+)/sanctions/events$"
    );
    private static final Pattern USER_TOPIC_PATTERN = Pattern.compile("^/topic/users/[^/]+/.*$");

    private UserWebSocketDestinations() {
    }

    public static String sanctionEvents(Long userId) {
        return "/topic/users/" + userId + "/sanctions/events";
    }

    public static Optional<Long> findSanctionEventUserId(String destination) {
        if (destination == null) {
            return Optional.empty();
        }

        Matcher matcher = SANCTION_EVENT_PATTERN.matcher(destination);
        if (!matcher.matches()) {
            return Optional.empty();
        }

        return Optional.of(Long.valueOf(matcher.group(1)));
    }

    public static boolean isUserTopic(String destination) {
        return destination != null && USER_TOPIC_PATTERN.matcher(destination).matches();
    }
}
