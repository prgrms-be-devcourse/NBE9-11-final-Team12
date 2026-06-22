package com.sisibibi.api.domain.usertrust.service;

import com.sisibibi.api.domain.roomparticipant.repository.RoomParticipantRepository;
import com.sisibibi.api.domain.speech.repository.SpeechRepository;
import com.sisibibi.api.domain.speechreport.repository.SpeechReportRepository;
import com.sisibibi.api.domain.speechreport.repository.ViolationHistorySummaryProjection;
import com.sisibibi.api.domain.speechreaction.repository.SpeechReactionRepository;
import com.sisibibi.api.domain.user.entity.User;
import com.sisibibi.api.domain.user.repository.UserRepository;
import com.sisibibi.api.global.exception.CustomException;
import com.sisibibi.api.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class UserTrustServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private SpeechReactionRepository speechReactionRepository;
    @Mock private SpeechReportRepository speechReportRepository;
    @Mock private SpeechRepository speechRepository;
    @Mock private RoomParticipantRepository roomParticipantRepository;
    @Mock private UserTrustPolicy userTrustPolicy;

    @InjectMocks private UserTrustService userTrustService;

    @Test
    void getMyTrust_returnsDetailedTrustInformation() {
        User user = user(1L, "tester");
        ViolationHistorySummaryProjection violations = violations(1, 2, 0, 0);
        UserTrustCalculation calculation = new UserTrustCalculation(
                52, 12, 10,
                com.sisibibi.api.domain.usertrust.entity.UserTrustLevel.NORMAL,
                com.sisibibi.api.domain.usertrust.entity.UserActivityLevel.ACTIVE
        );
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(speechReactionRepository.countReceivedByUserId(1L)).willReturn(12L);
        given(speechReportRepository.summarizeResolvedViolations(
                org.mockito.ArgumentMatchers.eq(1L),
                org.mockito.ArgumentMatchers.any()
        )).willReturn(violations);
        given(speechRepository.countByUserIdAndStatusAndDeletedFalse(
                1L,
                com.sisibibi.api.domain.speech.entity.SpeechStatus.COMPLETED
        )).willReturn(3L);
        given(roomParticipantRepository.countByUserId(1L)).willReturn(4L);
        given(userTrustPolicy.calculate(12, 1, 2, 0, 0, 3, 4))
                .willReturn(calculation);

        var response = userTrustService.getMyTrust(1L);

        assertThat(response.userId()).isEqualTo(1L);
        assertThat(response.score()).isEqualTo(52);
        assertThat(response.receivedReactionCount()).isEqualTo(12);
        assertThat(response.resolvedViolationCount()).isEqualTo(3);
    }

    @Test
    void getUserTrust_hidesViolationBreakdown() {
        User user = user(2L, "other");
        ViolationHistorySummaryProjection violations = violations(0, 0, 0, 0);
        given(userRepository.findById(2L)).willReturn(Optional.of(user));
        given(speechReactionRepository.countReceivedByUserId(2L)).willReturn(5L);
        given(speechReportRepository.summarizeResolvedViolations(
                org.mockito.ArgumentMatchers.eq(2L),
                org.mockito.ArgumentMatchers.any()
        )).willReturn(violations);
        given(speechRepository.countByUserIdAndStatusAndDeletedFalse(
                2L,
                com.sisibibi.api.domain.speech.entity.SpeechStatus.COMPLETED
        )).willReturn(1L);
        given(roomParticipantRepository.countByUserId(2L)).willReturn(2L);
        given(userTrustPolicy.calculate(5, 0, 0, 0, 0, 1, 2))
                .willReturn(new UserTrustCalculation(
                        55, 5, 0,
                        com.sisibibi.api.domain.usertrust.entity.UserTrustLevel.NORMAL,
                        com.sisibibi.api.domain.usertrust.entity.UserActivityLevel.ACTIVE
                ));

        var response = userTrustService.getUserTrust(2L);

        assertThat(response.userId()).isEqualTo(2L);
        assertThat(response.nickname()).isEqualTo("other");
        assertThat(response.score()).isEqualTo(55);
    }

    @Test
    void getUserTrust_throwsUserNotFound() {
        given(userRepository.findById(9L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> userTrustService.getUserTrust(9L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USER_NOT_FOUND);
    }

    private User user(Long id, String nickname) {
        User user = User.signup(nickname + "@example.com", "password", nickname);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private ViolationHistorySummaryProjection violations(
            long low,
            long medium,
            long high,
            long critical
    ) {
        ViolationHistorySummaryProjection projection =
                org.mockito.Mockito.mock(ViolationHistorySummaryProjection.class);
        given(projection.getLowCount()).willReturn(low);
        given(projection.getMediumCount()).willReturn(medium);
        given(projection.getHighCount()).willReturn(high);
        given(projection.getCriticalCount()).willReturn(critical);
        return projection;
    }
}
