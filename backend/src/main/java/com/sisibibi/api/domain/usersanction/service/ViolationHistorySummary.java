package com.sisibibi.api.domain.usersanction.service;

import com.sisibibi.api.domain.speechreport.repository.ViolationHistorySummaryProjection;

public record ViolationHistorySummary(
        long lowCount,
        long mediumCount,
        long highCount,
        long criticalCount
) {

    public static ViolationHistorySummary from(
            ViolationHistorySummaryProjection projection
    ) {
        return new ViolationHistorySummary(
                valueOrZero(projection.getLowCount()),
                valueOrZero(projection.getMediumCount()),
                valueOrZero(projection.getHighCount()),
                valueOrZero(projection.getCriticalCount())
        );
    }

    public long totalCount() {
        return lowCount + mediumCount + highCount + criticalCount;
    }

    public int weightedScore() {
        return Math.toIntExact(
                lowCount
                        + mediumCount * 2
                        + highCount * 4
                        + criticalCount * 8
        );
    }

    private static long valueOrZero(Long value) {
        return value == null ? 0 : value;
    }
}
