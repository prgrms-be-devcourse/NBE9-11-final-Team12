package com.sisibibi.api.domain.topic.dto.response;

import com.fasterxml.jackson.databind.JsonNode;

public record IssueNewsRes(
    String title,
    String originallink,
    String link,
    String description,
    String pubDate
) {

  public static IssueNewsRes from(JsonNode item) {
    return new IssueNewsRes(
        clean(item.path("title").asText()),
        item.path("originallink").asText(),
        item.path("link").asText(),
        clean(item.path("description").asText()),
        item.path("pubDate").asText()
    );
  }

  private static String clean(String value) {
    return value
        .replaceAll("</?b>", "")
        .replace("&quot;", "\"")
        .replace("&amp;", "&")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&#39;", "'");
  }
}