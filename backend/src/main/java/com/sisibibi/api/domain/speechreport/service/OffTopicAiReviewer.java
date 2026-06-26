package com.sisibibi.api.domain.speechreport.service;

import com.sisibibi.api.domain.speech.entity.Speech;

public interface OffTopicAiReviewer {

    OffTopicAiReviewResult review(Speech speech);
}
