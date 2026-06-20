package com.sisibibi.api.domain.topic.dto.response.keywordres;

import java.util.List;

public record ClassifiedNewsKeywordRes(
    int index,
    String category,
    List<String> keywords
) {
}