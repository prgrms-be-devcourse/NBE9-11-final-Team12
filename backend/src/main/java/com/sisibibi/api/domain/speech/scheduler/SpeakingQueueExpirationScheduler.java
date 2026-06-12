package com.sisibibi.api.domain.speech.scheduler;

import com.sisibibi.api.domain.speech.entity.SpeakingQueueStatus;
import com.sisibibi.api.domain.speech.repository.SpeakingQueueRepository;
import com.sisibibi.api.domain.speech.service.SpeakingQueueService;
import com.sisibibi.api.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        name = "app.speaking.expiration.enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class SpeakingQueueExpirationScheduler {

    @Value("${app.speaking.expiration.time-limit-seconds:300}")
    private long speakingTimeLimitSeconds;

    private final SpeakingQueueRepository speakingQueueRepository;
    private final SpeakingQueueService speakingQueueService;

    @Scheduled(fixedDelayString = "${app.speaking.expiration.scan-fixed-delay-ms:30000}")
    public void expireTimedOutSpeakers() {
        LocalDateTime now = LocalDateTime.now();
        Duration speakingTimeLimit = Duration.ofSeconds(speakingTimeLimitSeconds);
        LocalDateTime expiresBefore = now.minus(speakingTimeLimit);

        List<Long> roomIds = speakingQueueRepository
                .findDistinctRoomIdsByStatusAndAssignedAtLessThanEqual(
                        SpeakingQueueStatus.ASSIGNED,
                        expiresBefore
                );

        for (Long roomId : roomIds) {
            expireRoom(roomId, now, speakingTimeLimit);
        }
    }

    private void expireRoom(
            Long roomId,
            LocalDateTime now,
            Duration speakingTimeLimit
    ) {
        try {
            speakingQueueService.expireCurrentSpeakerIfTimedOut(roomId, now, speakingTimeLimit);
        } catch (CustomException e) {
            log.debug(
                    "Speaking expiration skipped. roomId={}, errorCode={}",
                    roomId,
                    e.getErrorCode()
            );
        }
    }
}
