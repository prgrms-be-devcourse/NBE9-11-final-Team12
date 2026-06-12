package com.sisibibi.api.global.client.naverApi;

import com.sisibibi.api.domain.topic.dto.request.NewsSearchCommand;
import com.sisibibi.api.domain.topic.dto.response.NewsSearchRes;

public interface NewsClient {

  NewsSearchRes search(NewsSearchCommand command);
}