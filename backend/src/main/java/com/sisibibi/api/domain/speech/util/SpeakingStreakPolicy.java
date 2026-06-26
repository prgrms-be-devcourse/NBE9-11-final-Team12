package com.sisibibi.api.domain.speech.util;


import com.sisibibi.api.domain.speech.entity.SpeakingQueue;
import com.sisibibi.api.domain.speech.entity.SpeechStance;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class SpeakingStreakPolicy {
    private static final int STREAK_THRESHOLD = 3;

    public Optional<SpeechStance> counterStanceFor(List<SpeakingQueue> recentAssignments){
        if (recentAssignments.size() < STREAK_THRESHOLD) {
            return Optional.empty();
        }
        List<SpeakingQueue> latestThree = recentAssignments.stream()
                .limit(STREAK_THRESHOLD)
                .toList();

        SpeechStance firstStance = latestThree.get(0).getStance();
        if (firstStance == null) {
            return Optional.empty();
        }

        boolean sameStanceStreak = latestThree.stream()
                .allMatch(queue -> firstStance == queue.getStance());

        if (!sameStanceStreak) {
            return Optional.empty();
        }

        return Optional.of(oppositeOf(firstStance));
    }

    private SpeechStance oppositeOf(SpeechStance stance) {
        return stance == SpeechStance.PRO ? SpeechStance.CON : SpeechStance.PRO;
    }
}
