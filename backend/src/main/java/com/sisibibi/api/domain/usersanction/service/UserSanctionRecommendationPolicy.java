package com.sisibibi.api.domain.usersanction.service;

import com.sisibibi.api.domain.speechreport.entity.ViolationSeverity;
import com.sisibibi.api.domain.usersanction.entity.UserSanctionType;
import org.springframework.stereotype.Component;

@Component
public class UserSanctionRecommendationPolicy {

    private static final int ONE_DAY_HOURS = 24;
    private static final int SEVEN_DAYS_HOURS = 168;
    private static final int THIRTY_DAYS_HOURS = 720;

    public SanctionRecommendation recommend(
            ViolationSeverity currentSeverity,
            ViolationHistorySummary history
    ) {
        int weightedScore = history.weightedScore();

        if (currentSeverity == ViolationSeverity.CRITICAL) {
            return restriction(
                    THIRTY_DAYS_HOURS,
                    "중대한 위반이 확인되어 30일 의견 제한을 추천합니다."
            );
        }
        if (currentSeverity == ViolationSeverity.HIGH) {
            return restriction(
                    SEVEN_DAYS_HOURS,
                    "높은 심각도의 위반이 확인되어 7일 의견 제한을 추천합니다."
            );
        }
        if (weightedScore >= 8) {
            return restriction(
                    SEVEN_DAYS_HOURS,
                    "최근 90일 누적 위반 점수가 8점 이상입니다."
            );
        }
        if (weightedScore >= 4) {
            return restriction(
                    ONE_DAY_HOURS,
                    "최근 90일 누적 위반 점수가 4점 이상입니다."
            );
        }
        return new SanctionRecommendation(
                UserSanctionType.WARNING,
                null,
                "최근 90일 위반 이력이 경고 단계에 해당합니다."
        );
    }

    private SanctionRecommendation restriction(int durationHours, String reason) {
        return new SanctionRecommendation(
                UserSanctionType.SPEECH_RESTRICTION,
                durationHours,
                reason
        );
    }
}
