package com.sisibibi.api.domain.topic.dto.response.issueRes;

public record IssueNewsRes(
    String title,
    String originallink,
    String link,
    String description,
    String pubDate
) {

  public IssueNewsRes {
    title = clean(title);
    description = clean(description);
  }

  private static String clean(String value) {
    if (value == null) {
      return "";
    }

    return value
        .replaceAll("</?b>", "")
        .replace("&quot;", "\"")
        .replace("&amp;", "&")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&#39;", "'");
  }
}