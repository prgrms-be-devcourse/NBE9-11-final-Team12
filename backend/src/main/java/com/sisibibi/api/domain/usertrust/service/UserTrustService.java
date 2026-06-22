package com.sisibibi.api.domain.usertrust.service;

import com.sisibibi.api.domain.roomparticipant.repository.RoomParticipantRepository;
import com.sisibibi.api.domain.speech.entity.SpeechStatus;
import com.sisibibi.api.domain.speech.repository.SpeechRepository;
import com.sisibibi.api.domain.speechreaction.repository.SpeechReactionRepository;
import com.sisibibi.api.domain.speechreport.repository.SpeechReportRepository;
import com.sisibibi.api.domain.speechreport.repository.ViolationHistorySummaryProjection;
import com.sisibibi.api.domain.user.entity.User;
import com.sisibibi.api.domain.user.repository.UserRepository;
import com.sisibibi.api.domain.usertrust.dto.response.UserTrustDetailRes;
import com.sisibibi.api.domain.usertrust.dto.response.UserTrustSummaryRes;
import com.sisibibi.api.global.exception.CustomException;
import com.sisibibi.api.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserTrustService {

    private static final int VIOLATION_LOOKBACK_DAYS = 90;

    private final UserRepository userRepository;
    private final SpeechReactionRepository speechReactionRepository;
    private final SpeechReportRepository speechReportRepository;
    private final SpeechRepository speechRepository;
    private final RoomParticipantRepository roomParticipantRepository;
    private final UserTrustPolicy userTrustPolicy;

    @Transactional(readOnly = true)
    public UserTrustDetailRes getMyTrust(Long userId) {
        TrustContext context = calculateTrust(userId);
        long violationCount = context.lowViolationCount()
                + context.mediumViolationCount()
                + context.highViolationCount()
                + context.criticalViolationCount();

        return new UserTrustDetailRes(
                context.user().getId(),
                context.user().getNickname(),
                context.calculation().score(),
                context.calculation().trustLevel(),
                context.calculation().activityLevel(),
                context.receivedReactionCount(),
                context.completedSpeechCount(),
                context.participatedRoomCount(),
                violationCount,
                context.calculation().positiveScore(),
                context.calculation().penaltyScore()
        );
    }

    @Transactional(readOnly = true)
    public UserTrustSummaryRes getUserTrust(Long userId) {
        TrustContext context = calculateTrust(userId);

        return new UserTrustSummaryRes(
                context.user().getId(),
                context.user().getNickname(),
                context.calculation().score(),
                context.calculation().trustLevel(),
                context.calculation().activityLevel()
        );
    }

    private TrustContext calculateTrust(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        long receivedReactionCount = speechReactionRepository.countReceivedByUserId(userId);
        ViolationHistorySummaryProjection violations =
                speechReportRepository.summarizeResolvedViolations(
                        userId,
                        LocalDateTime.now().minusDays(VIOLATION_LOOKBACK_DAYS)
                );
        long lowViolationCount = value(violations.getLowCount());
        long mediumViolationCount = value(violations.getMediumCount());
        long highViolationCount = value(violations.getHighCount());
        long criticalViolationCount = value(violations.getCriticalCount());
        long completedSpeechCount = speechRepository.countByUserIdAndStatusAndDeletedFalse(
                userId,
                SpeechStatus.COMPLETED
        );
        long participatedRoomCount = roomParticipantRepository.countByUserId(userId);
        UserTrustCalculation calculation = userTrustPolicy.calculate(
                receivedReactionCount,
                lowViolationCount,
                mediumViolationCount,
                highViolationCount,
                criticalViolationCount,
                completedSpeechCount,
                participatedRoomCount
        );

        return new TrustContext(
                user,
                receivedReactionCount,
                lowViolationCount,
                mediumViolationCount,
                highViolationCount,
                criticalViolationCount,
                completedSpeechCount,
                participatedRoomCount,
                calculation
        );
    }

    private long value(Long count) {
        return count == null ? 0 : count;
    }

    private record TrustContext(
            User user,
            long receivedReactionCount,
            long lowViolationCount,
            long mediumViolationCount,
            long highViolationCount,
            long criticalViolationCount,
            long completedSpeechCount,
            long participatedRoomCount,
            UserTrustCalculation calculation
    ) {
    }
}
