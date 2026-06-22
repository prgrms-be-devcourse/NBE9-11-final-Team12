package com.sisibibi.api.domain.speechreport.repository;

public interface ViolationHistorySummaryProjection {

    Long getLowCount();

    Long getMediumCount();

    Long getHighCount();

    Long getCriticalCount();
}
