package com.sisibibi.api.domain.speech.scheduler;

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
        prefix = "app.speaking.expiration",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class SpeakingQueueExpirationScheduler {

    private final SpeakingQueueRepository speakingQueueRepository;
    private final SpeakingQueueService speakingQueueService;

    @Scheduled(
            fixedDelayString =
                    "${app.speaking.expiration.fixed-delay-ms:10000}"
    )
    public void expireSpeakers() {
        expireSpeakersAt(LocalDateTime.now());
    }

    void expireSpeakersAt(LocalDateTime now) {
        for (Long roomId :
                speakingQueueRepository.findRoomIdsWithExpiredSpeaker(now)) {
            try {
                speakingQueueService.expireCurrentSpeaker(roomId, now);
            } catch (RuntimeException expirationException) {
                log.error(
                        "Failed to expire current speaker. roomId={}",
                        roomId,
                        expirationException
                );
            }
        }
    }
}
