package com.sisibibi.api.domain.topic.dto.response.issueRes;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TrendingSearchRes(
    String query,

    @JsonProperty("search_volume")
    long searchVolume,

    @JsonProperty("increase_percentage")
    int increasePercentage
) {
}
