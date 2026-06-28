package com.sisibibi.api.global.websocket.presence;

public record RoomPresenceCandidate(
        Long roomId,
        Long userId,
        long generation
) {

    public String member() {
        return roomId + ":" + userId + ":" + generation;
    }

    public static RoomPresenceCandidate parse(String member) {
        String[] parts = member.split(":");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Invalid room presence candidate: " + member);
        }
        try {
            return new RoomPresenceCandidate(
                    Long.valueOf(parts[0]),
                    Long.valueOf(parts[1]),
                    Long.parseLong(parts[2])
            );
        } catch (NumberFormatException parseException) {
            throw new IllegalArgumentException(
                    "Invalid room presence candidate number: " + member,
                    parseException
            );
        }
    }
}
