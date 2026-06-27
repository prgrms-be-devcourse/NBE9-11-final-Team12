package com.sisibibi.api.domain.speech.service;

import java.util.List;

public record StageSummaryResult(
        String moderatorSummary,
        List<String> keyPoints
) {
}
