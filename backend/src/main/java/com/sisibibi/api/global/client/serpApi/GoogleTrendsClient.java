package com.sisibibi.api.global.client.serpApi;

import com.fasterxml.jackson.databind.JsonNode;
import com.sisibibi.api.domain.topic.dto.response.GoogleTrendsRes;

public interface GoogleTrendsClient {

  GoogleTrendsRes getTrendingNow();
}