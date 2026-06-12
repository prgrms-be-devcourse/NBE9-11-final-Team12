package com.sisibibi.api.global.client.naverApi;

import com.fasterxml.jackson.databind.JsonNode;
import com.sisibibi.api.domain.topic.dto.request.NewsSearchCommand;

public interface NewsClient {

  JsonNode search(NewsSearchCommand command);
}