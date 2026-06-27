package com.sisibibi.api.domain.topic.scheduler;

import com.sisibibi.api.domain.topic.service.TopicCandidateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TopicCandidateScheduler {

  private final TopicCandidateService topicCandidateCacheService;

  @Scheduled(cron = "0 0 */3 * * *", zone = "Asia/Seoul")
  public void refreshTopicCandidates() {
    log.info("Topic candidate refresh started.");

    int candidateCount = topicCandidateCacheService
        .refreshLatestClassifiedCandidates()
        .size();

    log.info("Topic candidate refresh completed. candidateCount={}", candidateCount);
  }
}