package com.sisibibi.api.domain.topic.dto.response.keywordres;

import java.util.List;

public record NewsKeywordClassificationResult(
    List<ClassifiedNewsKeywordRes> news
) {
}
