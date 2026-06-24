package com.sisibibi.api.domain.usersanction.dto.event;

public record UserSanctionChangedEvent(
        UserSanctionEventType type,
        Long userId,
        UserSanctionEventPayload payload
) {
}
