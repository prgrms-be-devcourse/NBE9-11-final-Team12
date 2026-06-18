package com.sisibibi.api.domain.chat.dto.response;

import java.util.List;

public record ChatMessageCursorPageRes(
        List<ChatMessageRes> items,
        Long nextCursor,
        boolean hasNext
) {
}
