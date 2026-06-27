package com.sisibibi.api.domain.usersanction.service;

import com.sisibibi.api.domain.speechreport.entity.ViolationSeverity;
import com.sisibibi.api.domain.usersanction.entity.UserSanctionType;
import org.springframework.stereotype.Component;

@Component
public class UserSanctionRecommendationPolicy {

    private static final int ONE_DAY_HOURS = 24;
    private static final int SEVEN_DAYS_HOURS = 168;
    private static final int THIRTY_DAYS_HOURS = 720;
    private static final int ACCOUNT_SUSPENSION_REVIEW_SCORE = 24;
    private static final int ACCOUNT_SUSPENSION_REVIEW_CRITICAL_COUNT = 2;

    public SanctionRecommendation recommend(
            ViolationSeverity currentSeverity,
            ViolationHistorySummary history
    ) {
        int weightedScore = history.weightedScore();
        boolean accountSuspensionReviewRecommended =
                weightedScore >= ACCOUNT_SUSPENSION_REVIEW_SCORE
                        || history.criticalCount() >= ACCOUNT_SUSPENSION_REVIEW_CRITICAL_COUNT;

        if (currentSeverity == ViolationSeverity.CRITICAL) {
            return restriction(
                    THIRTY_DAYS_HOURS,
                    accountSuspensionReviewRecommended,
                    "중대한 위반이 확인되어 30일 발언/의견 제한을 추천합니다."
            );
        }
        if (currentSeverity == ViolationSeverity.HIGH) {
            return restriction(
                    SEVEN_DAYS_HOURS,
                    accountSuspensionReviewRecommended,
                    "높은 심각도의 위반이 확인되어 7일 발언/의견 제한을 추천합니다."
            );
        }
        if (weightedScore >= 8) {
            return restriction(
                    SEVEN_DAYS_HOURS,
                    accountSuspensionReviewRecommended,
                    "최근 90일 누적 위반 점수가 8점 이상입니다."
            );
        }
        if (weightedScore >= 4) {
            return restriction(
                    ONE_DAY_HOURS,
                    accountSuspensionReviewRecommended,
                    "최근 90일 누적 위반 점수가 4점 이상입니다."
            );
        }
        return new SanctionRecommendation(
                UserSanctionType.WARNING,
                null,
                accountSuspensionReviewRecommended,
                "최근 90일 위반 이력이 경고 단계에 해당합니다."
        );
    }

    private SanctionRecommendation restriction(
            int durationHours,
            boolean accountSuspensionReviewRecommended,
            String reason
    ) {
        return new SanctionRecommendation(
                UserSanctionType.STAGE_RESTRICTION,
                durationHours,
                accountSuspensionReviewRecommended,
                reason
        );
    }
}
