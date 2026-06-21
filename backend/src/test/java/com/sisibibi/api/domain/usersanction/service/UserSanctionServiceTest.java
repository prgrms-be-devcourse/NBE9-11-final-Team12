package com.sisibibi.api.domain.usersanction.service;

import com.sisibibi.api.domain.speechreport.entity.SpeechReport;
import com.sisibibi.api.domain.speechreport.entity.SpeechReportReason;
import com.sisibibi.api.domain.speechreport.entity.SpeechReportStatus;
import com.sisibibi.api.domain.speechreport.repository.SpeechReportRepository;
import com.sisibibi.api.domain.user.entity.User;
import com.sisibibi.api.domain.user.repository.UserRepository;
import com.sisibibi.api.domain.usersanction.dto.request.UserSanctionCreateReq;
import com.sisibibi.api.domain.usersanction.dto.event.UserSanctionChangedEvent;
import com.sisibibi.api.domain.usersanction.dto.response.UserSanctionRes;
import com.sisibibi.api.domain.usersanction.entity.UserSanction;
import com.sisibibi.api.domain.usersanction.entity.UserSanctionState;
import com.sisibibi.api.domain.usersanction.entity.UserSanctionType;
import com.sisibibi.api.domain.usersanction.repository.UserSanctionRepository;
import com.sisibibi.api.global.exception.CustomException;
import com.sisibibi.api.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserSanctionServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private SpeechReportRepository speechReportRepository;

    @Mock
    private UserSanctionRepository userSanctionRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private UserSanctionService userSanctionService;

    @Test
    void createSanction_savesRestrictionLinkedToResolvedReport() {
        User user = User.signup("user@example.com", "password", "user");
        SpeechReport report = resolvedReport(100L, 10L);
        given(userRepository.findByIdForUpdate(10L)).willReturn(Optional.of(user));
        given(speechReportRepository.findById(100L)).willReturn(Optional.of(report));
        given(userSanctionRepository.existsActive(
                org.mockito.ArgumentMatchers.eq(10L),
                org.mockito.ArgumentMatchers.eq(UserSanctionType.CHAT_RESTRICTION),
                any(LocalDateTime.class)
        )).willReturn(false);
        given(userSanctionRepository.save(any(UserSanction.class)))
                .willAnswer(invocation -> {
                    UserSanction sanction = invocation.getArgument(0);
                    ReflectionTestUtils.setField(sanction, "id", 200L);
                    ReflectionTestUtils.setField(sanction, "createdAt", LocalDateTime.now());
                    return sanction;
                });

        UserSanctionRes response = userSanctionService.createSanction(
                10L,
                99L,
                new UserSanctionCreateReq(
                        UserSanctionType.CHAT_RESTRICTION,
                        "반복적인 채팅 도배",
                        24,
                        100L
                )
        );

        assertThat(response.sanctionId()).isEqualTo(200L);
        assertThat(response.reportId()).isEqualTo(100L);
        assertThat(response.state()).isEqualTo(UserSanctionState.ACTIVE);
        verify(eventPublisher).publishEvent(any(UserSanctionChangedEvent.class));
    }

    @Test
    void createSanction_throwsAlreadyActive_whenSameRestrictionExists() {
        given(userRepository.findByIdForUpdate(10L))
                .willReturn(Optional.of(User.signup("user@example.com", "password", "user")));
        given(userSanctionRepository.existsActive(
                org.mockito.ArgumentMatchers.eq(10L),
                org.mockito.ArgumentMatchers.eq(UserSanctionType.SPEECH_RESTRICTION),
                any(LocalDateTime.class)
        )).willReturn(true);

        assertThatThrownBy(() -> userSanctionService.createSanction(
                10L,
                99L,
                new UserSanctionCreateReq(
                        UserSanctionType.SPEECH_RESTRICTION,
                        "반복 위반",
                        24,
                        null
                )
        ))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USER_SANCTION_ALREADY_ACTIVE);
    }

    @Test
    void createSanction_throwsAdminNotAllowed_whenTargetIsAdmin() {
        given(userRepository.findByIdForUpdate(10L))
                .willReturn(Optional.of(User.admin(
                        "admin@example.com",
                        "password",
                        "admin"
                )));

        assertThatThrownBy(() -> userSanctionService.createSanction(
                10L,
                99L,
                new UserSanctionCreateReq(
                        UserSanctionType.WARNING,
                        "경고",
                        null,
                        null
                )
        ))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USER_SANCTION_ADMIN_NOT_ALLOWED);
    }

    @Test
    void createSanction_throwsReportMismatch_whenReportedUserDiffers() {
        given(userRepository.findByIdForUpdate(10L))
                .willReturn(Optional.of(User.signup("user@example.com", "password", "user")));
        given(speechReportRepository.findById(100L))
                .willReturn(Optional.of(resolvedReport(100L, 11L)));

        assertThatThrownBy(() -> userSanctionService.createSanction(
                10L,
                99L,
                new UserSanctionCreateReq(
                        UserSanctionType.WARNING,
                        "경고",
                        null,
                        100L
                )
        ))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USER_SANCTION_REPORT_MISMATCH);
    }

    @Test
    void getSanctions_returnsHistory() {
        PageRequest pageable = PageRequest.of(0, 20);
        UserSanction sanction = sanction(200L);
        given(userRepository.existsById(10L)).willReturn(true);
        given(userSanctionRepository.findByUserIdOrderByCreatedAtDescIdDesc(10L, pageable))
                .willReturn(new PageImpl<>(List.of(sanction), pageable, 1));

        assertThat(userSanctionService.getSanctions(10L, pageable).getTotalElements())
                .isEqualTo(1);
    }

    @Test
    void getActiveSanctions_returnsCurrentRestrictionsWithoutInternalData() {
        UserSanction sanction = sanction(200L);
        given(userSanctionRepository.findActiveRestrictions(
                org.mockito.ArgumentMatchers.eq(10L),
                any(LocalDateTime.class)
        )).willReturn(List.of(sanction));

        var response = userSanctionService.getActiveSanctions(10L);

        assertThat(response).hasSize(1);
        assertThat(response.getFirst().sanctionId()).isEqualTo(200L);
        assertThat(response.getFirst().type())
                .isEqualTo(UserSanctionType.CHAT_RESTRICTION);
    }

    @Test
    void revokeSanction_revokesActiveSanction() {
        UserSanction sanction = sanction(200L);
        given(userSanctionRepository.findByIdAndUserIdForUpdate(200L, 10L))
                .willReturn(Optional.of(sanction));

        UserSanctionRes response =
                userSanctionService.revokeSanction(10L, 200L, 100L, "오인 제재");

        assertThat(response.state()).isEqualTo(UserSanctionState.REVOKED);
        verify(userSanctionRepository).findByIdAndUserIdForUpdate(200L, 10L);
        verify(eventPublisher).publishEvent(any(UserSanctionChangedEvent.class));
    }

    private SpeechReport resolvedReport(Long reportId, Long reportedUserId) {
        SpeechReport report = SpeechReport.create(
                1L,
                reportedUserId,
                20L,
                "신고 대상 의견",
                SpeechReportReason.SPAM,
                null
        );
        ReflectionTestUtils.setField(report, "id", reportId);
        ReflectionTestUtils.setField(report, "status", SpeechReportStatus.RESOLVED);
        return report;
    }

    private UserSanction sanction(Long sanctionId) {
        LocalDateTime now = LocalDateTime.now();
        UserSanction sanction = UserSanction.create(
                10L,
                99L,
                null,
                UserSanctionType.CHAT_RESTRICTION,
                "채팅 제한",
                now.minusMinutes(1),
                now.plusHours(24)
        );
        ReflectionTestUtils.setField(sanction, "id", sanctionId);
        ReflectionTestUtils.setField(sanction, "createdAt", now);
        return sanction;
    }
}
