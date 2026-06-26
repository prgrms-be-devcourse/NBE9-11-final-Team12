package com.sisibibi.api.domain.usersanction.service;

import com.sisibibi.api.domain.speechreport.entity.SpeechReport;
import com.sisibibi.api.domain.speechreport.entity.SpeechReportReason;
import com.sisibibi.api.domain.speechreport.entity.SpeechReportStatus;
import com.sisibibi.api.domain.speechreport.entity.ViolationSeverity;
import com.sisibibi.api.domain.speechreport.repository.SpeechReportRepository;
import com.sisibibi.api.domain.speechreport.repository.ViolationHistorySummaryProjection;
import com.sisibibi.api.domain.usersanction.entity.UserSanctionType;
import com.sisibibi.api.domain.usersanction.repository.UserSanctionRepository;
import com.sisibibi.api.global.exception.CustomException;
import com.sisibibi.api.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class UserSanctionRecommendationServiceTest {

    @Mock
    private SpeechReportRepository speechReportRepository;

    @Mock
    private UserSanctionRepository userSanctionRepository;

    @Mock
    private UserSanctionRecommendationPolicy recommendationPolicy;

    @InjectMocks
    private UserSanctionRecommendationService recommendationService;

    @Test
    void recommend_returnsRecommendationFromResolvedHistory() {
        SpeechReport report = resolvedReport(100L, 10L, ViolationSeverity.MEDIUM);
        ViolationHistorySummaryProjection projection = projection(1L, 2L, 0L, 0L);
        given(speechReportRepository.findById(100L)).willReturn(Optional.of(report));
        given(speechReportRepository.summarizeResolvedViolations(
                eq(10L),
                any(LocalDateTime.class)
        )).willReturn(projection);
        given(recommendationPolicy.recommend(
                ViolationSeverity.MEDIUM,
                new ViolationHistorySummary(1, 2, 0, 0)
        )).willReturn(new SanctionRecommendation(
                UserSanctionType.SPEECH_RESTRICTION,
                24,
                false,
                "최근 90일 누적 위반 점수가 4점 이상입니다."
        ));
        given(userSanctionRepository.findFirstActive(
                eq(10L),
                eq(UserSanctionType.SPEECH_RESTRICTION),
                any(LocalDateTime.class)
        )).willReturn(Optional.of(activeSanction()));

        var response = recommendationService.recommend(10L, 100L);

        assertThat(response.resolvedViolationCount()).isEqualTo(3);
        assertThat(response.lowCount()).isEqualTo(1);
        assertThat(response.mediumCount()).isEqualTo(2);
        assertThat(response.weightedScore()).isEqualTo(5);
        assertThat(response.recommendedDurationHours()).isEqualTo(24);
        assertThat(response.accountSuspensionReviewRecommended()).isFalse();
        assertThat(response.activeSameTypeSanction()).isTrue();
        assertThat(response.activeSameTypeSanctionId()).isEqualTo(200L);
    }

    @Test
    void recommend_throwsReportNotResolved_whenReportIsPending() {
        SpeechReport report = SpeechReport.create(
                1L,
                10L,
                20L,
                "신고 대상 의견",
                SpeechReportReason.SPAM,
                null
        );
        given(speechReportRepository.findById(100L)).willReturn(Optional.of(report));

        assertThatThrownBy(() -> recommendationService.recommend(10L, 100L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USER_SANCTION_REPORT_NOT_RESOLVED);
    }

    @Test
    void recommend_throwsReportMismatch_whenTargetUserDiffers() {
        SpeechReport report = resolvedReport(100L, 11L, ViolationSeverity.LOW);
        given(speechReportRepository.findById(100L)).willReturn(Optional.of(report));

        assertThatThrownBy(() -> recommendationService.recommend(10L, 100L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USER_SANCTION_REPORT_MISMATCH);
    }

    private SpeechReport resolvedReport(
            Long reportId,
            Long userId,
            ViolationSeverity severity
    ) {
        SpeechReport report = SpeechReport.create(
                1L,
                userId,
                20L,
                "신고 대상 의견",
                SpeechReportReason.SPAM,
                null
        );
        ReflectionTestUtils.setField(report, "id", reportId);
        ReflectionTestUtils.setField(report, "status", SpeechReportStatus.RESOLVED);
        ReflectionTestUtils.setField(report, "severity", severity);
        ReflectionTestUtils.setField(report, "reviewedAt", LocalDateTime.now());
        return report;
    }

    private ViolationHistorySummaryProjection projection(
            Long low,
            Long medium,
            Long high,
            Long critical
    ) {
        return new ViolationHistorySummaryProjection() {
            @Override
            public Long getLowCount() {
                return low;
            }

            @Override
            public Long getMediumCount() {
                return medium;
            }

            @Override
            public Long getHighCount() {
                return high;
            }

            @Override
            public Long getCriticalCount() {
                return critical;
            }
        };
    }

    private com.sisibibi.api.domain.usersanction.entity.UserSanction activeSanction() {
        LocalDateTime now = LocalDateTime.now();
        var sanction = com.sisibibi.api.domain.usersanction.entity.UserSanction.create(
                10L,
                99L,
                null,
                UserSanctionType.SPEECH_RESTRICTION,
                "의견 제한",
                now.minusHours(1),
                now.plusHours(24)
        );
        ReflectionTestUtils.setField(sanction, "id", 200L);
        return sanction;
    }
}
