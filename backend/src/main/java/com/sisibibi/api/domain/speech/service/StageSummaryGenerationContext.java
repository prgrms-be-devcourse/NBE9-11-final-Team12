package com.sisibibi.api.domain.speech.service;

import com.sisibibi.api.domain.room.entity.Room;
import com.sisibibi.api.domain.speech.entity.Speech;

import java.util.List;

public record StageSummaryGenerationContext(
        boolean shouldCallAi,
        Long summaryId,
        Room room,
        List<Speech> speeches
) {

    public static StageSummaryGenerationContext skip() {
        return new StageSummaryGenerationContext(false, null, null, List.of());
    }

    public static StageSummaryGenerationContext callAi(
            Long summaryId,
            Room room,
            List<Speech> speeches
    ) {
        return new StageSummaryGenerationContext(
                true,
                summaryId,
                room,
                speeches == null ? List.of() : List.copyOf(speeches)
        );
    }
}
