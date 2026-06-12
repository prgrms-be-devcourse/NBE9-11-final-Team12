package com.sisibibi.api.domain.speech.dto.response;

import java.util.List;

public record SpeechCursorPageRes(
        List<SpeechListRes> items,
        Long nextCursor,
        boolean hasNext
) {
}
