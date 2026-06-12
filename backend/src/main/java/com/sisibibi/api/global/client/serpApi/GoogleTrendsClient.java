package com.sisibibi.api.global.client.serpApi;

import com.sisibibi.api.domain.topic.dto.response.issueRes.GoogleTrendsRes;

public interface GoogleTrendsClient {

  GoogleTrendsRes getTrendingNow();
}