package com.sisibibi.api.domain.usersanction.service;

import com.sisibibi.api.domain.speechreport.entity.SpeechReport;
import com.sisibibi.api.domain.speechreport.entity.SpeechReportStatus;
import com.sisibibi.api.domain.speechreport.repository.SpeechReportRepository;
import com.sisibibi.api.domain.speechreport.repository.ViolationHistorySummaryProjection;
import com.sisibibi.api.domain.usersanction.dto.response.UserSanctionRecommendationRes;
import com.sisibibi.api.domain.usersanction.entity.UserSanction;
import com.sisibibi.api.domain.usersanction.entity.UserSanctionType;
import com.sisibibi.api.domain.usersanction.repository.UserSanctionRepository;
import com.sisibibi.api.global.exception.CustomException;
import com.sisibibi.api.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserSanctionRecommendationService {

    private static final int LOOKBACK_DAYS = 90;

    private final SpeechReportRepository speechReportRepository;
    private final UserSanctionRepository userSanctionRepository;
    private final UserSanctionRecommendationPolicy recommendationPolicy;

    @Transactional(readOnly = true)
    public UserSanctionRecommendationRes recommend(Long userId, Long reportId) {
        SpeechReport report = speechReportRepository.findById(reportId)
                .orElseThrow(() -> new CustomException(ErrorCode.SPEECH_REPORT_NOT_FOUND));
        validateReport(userId, report);

        LocalDateTime now = LocalDateTime.now();
        ViolationHistorySummaryProjection projection =
                speechReportRepository.summarizeResolvedViolations(
                        userId,
                        now.minusDays(LOOKBACK_DAYS)
                );
        ViolationHistorySummary history = ViolationHistorySummary.from(projection);
        SanctionRecommendation recommendation =
                recommendationPolicy.recommend(report.getSeverity(), history);
        UserSanction activeSameTypeSanction = findActiveSameTypeSanction(
                userId,
                recommendation.type(),
                now
        );

        return new UserSanctionRecommendationRes(
                reportId,
                userId,
                report.getSeverity(),
                LOOKBACK_DAYS,
                history.totalCount(),
                history.lowCount(),
                history.mediumCount(),
                history.highCount(),
                history.criticalCount(),
                history.weightedScore(),
                recommendation.type(),
                recommendation.durationHours(),
                activeSameTypeSanction != null,
                activeSameTypeSanction == null ? null : activeSameTypeSanction.getId(),
                activeSameTypeSanction == null ? null : activeSameTypeSanction.getEndsAt(),
                recommendation.reason()
        );
    }

    private void validateReport(Long userId, SpeechReport report) {
        if (report.getStatus() != SpeechReportStatus.RESOLVED) {
            throw new CustomException(ErrorCode.USER_SANCTION_REPORT_NOT_RESOLVED);
        }
        if (!report.getReportedUserId().equals(userId)) {
            throw new CustomException(ErrorCode.USER_SANCTION_REPORT_MISMATCH);
        }
    }

    private UserSanction findActiveSameTypeSanction(
            Long userId,
            UserSanctionType type,
            LocalDateTime now
    ) {
        if (type == UserSanctionType.WARNING) {
            return null;
        }
        return userSanctionRepository.findFirstActive(userId, type, now)
                .orElse(null);
    }
}
