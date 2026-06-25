package com.sisibibi.api.domain.speech.scheduler;

import com.sisibibi.api.domain.speech.config.SpeakingQueueProperties;
import com.sisibibi.api.domain.speech.repository.SpeakingQueueRepository;
import com.sisibibi.api.domain.speech.service.SpeakingQueueService;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "app.speaking.idle",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class SpeakingQueueIdleScheduler {

    private final SpeakingQueueRepository speakingQueueRepository;
    private final SpeakingQueueService speakingQueueService;
    private final SpeakingQueueProperties speakingQueueProperties;

    @Scheduled(
            fixedDelayString =
                    "${app.speaking.idle.fixed-delay-ms:5000}"
    )
    public void handleIdleSpeakers() {
        handleIdleSpeakersAt(LocalDateTime.now());
    }

    void handleIdleSpeakersAt(LocalDateTime now) {
        SpeakingQueueProperties.Idle idleProperties = speakingQueueProperties.getIdle();

        for (Long roomId : speakingQueueRepository.findRoomIdsRequiringIdleWarning(
                now,
                idleProperties.getWarningDelay(),
                idleProperties.getWarningSuppressionBeforeExpiration()
        )) {
            try {
                speakingQueueService.warnIdleCurrentSpeaker(roomId, now);
            } catch (RuntimeException idleWarningException) {
                log.error(
                        "Failed to warn idle speaker. roomId={}",
                        roomId,
                        idleWarningException
                );
            }
        }

        for (Long roomId : speakingQueueRepository.findRoomIdsWithIdleTimedOutSpeaker(
                now,
                idleProperties.getTimeoutDelayAfterWarning()
        )) {
            try {
                speakingQueueService.completeIdleCurrentSpeaker(roomId, now);
            } catch (RuntimeException idleTimeoutException) {
                log.error(
                        "Failed to complete idle speaker. roomId={}",
                        roomId,
                        idleTimeoutException
                );
            }
        }
    }
}
