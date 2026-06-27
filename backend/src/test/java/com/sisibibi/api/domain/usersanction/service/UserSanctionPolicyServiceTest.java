package com.sisibibi.api.domain.usersanction.service;

import com.sisibibi.api.domain.usersanction.entity.UserSanctionType;
import com.sisibibi.api.domain.usersanction.repository.UserSanctionRepository;
import com.sisibibi.api.global.exception.CustomException;
import com.sisibibi.api.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class UserSanctionPolicyServiceTest {

    @Mock
    private UserSanctionRepository userSanctionRepository;

    @InjectMocks
    private UserSanctionPolicyService userSanctionPolicyService;

    @Test
    void validateChatAllowed_throwsRestricted_whenActiveRestrictionExists() {
        given(userSanctionRepository.existsActive(
                eq(10L),
                eq(UserSanctionType.CHAT_RESTRICTION),
                any(LocalDateTime.class)
        )).willReturn(true);

        assertThatThrownBy(() -> userSanctionPolicyService.validateChatAllowed(10L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USER_CHAT_RESTRICTED);
    }

    @Test
    void validateSpeechAllowed_throwsRestricted_whenActiveRestrictionExists() {
        given(userSanctionRepository.existsActiveIn(
                eq(10L),
                eq(List.of(UserSanctionType.SPEECH_RESTRICTION, UserSanctionType.STAGE_RESTRICTION)),
                any(LocalDateTime.class)
        )).willReturn(true);

        assertThatThrownBy(() -> userSanctionPolicyService.validateSpeechAllowed(10L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USER_SPEECH_RESTRICTED);
    }

    @Test
    void validateStageAllowed_throwsRestricted_whenActiveRestrictionExists() {
        given(userSanctionRepository.existsActiveIn(
                eq(10L),
                eq(List.of(UserSanctionType.SPEECH_RESTRICTION, UserSanctionType.STAGE_RESTRICTION)),
                any(LocalDateTime.class)
        )).willReturn(true);

        assertThatThrownBy(() -> userSanctionPolicyService.validateStageAllowed(10L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USER_STAGE_RESTRICTED);
    }
}
