package com.sisibibi.api.global.client.serpApi;

import com.fasterxml.jackson.databind.JsonNode;

public interface GoogleTrendsClient {

  JsonNode getKoreaTrendingNow();
}