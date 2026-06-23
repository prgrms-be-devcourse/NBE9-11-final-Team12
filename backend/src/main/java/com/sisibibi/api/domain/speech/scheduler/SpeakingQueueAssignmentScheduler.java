package com.sisibibi.api.domain.speech.scheduler;

import com.sisibibi.api.domain.speech.repository.SpeakingQueueRepository;
import com.sisibibi.api.domain.speech.service.SpeakingQueueService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "app.speaking.assignment",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class SpeakingQueueAssignmentScheduler {

    private final SpeakingQueueRepository speakingQueueRepository;
    private final SpeakingQueueService speakingQueueService;

    @Scheduled(
            fixedDelayString =
                    "${app.speaking.assignment.fixed-delay-ms:100000}"
    )
    public void assignWaitingSpeakers() {
        for (Long roomId :
                speakingQueueRepository.findRoomIdsRequiringAssignment()) {
            try {
                speakingQueueService.assignNextSpeaker(roomId);
            } catch (RuntimeException assignmentException) {
                log.error(
                        "Failed to assign next speaker. roomId={}",
                        roomId,
                        assignmentException
                );
            }
        }
    }
}
