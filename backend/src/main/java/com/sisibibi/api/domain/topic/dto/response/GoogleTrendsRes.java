package com.sisibibi.api.domain.topic.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record GoogleTrendsRes(
    @JsonProperty("trending_searches")
    List<TrendingSearchRes> trendingSearches
) {
}