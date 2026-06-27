package com.sisibibi.api.domain.usersanction.service;

import com.sisibibi.api.domain.usersanction.entity.UserSanctionType;
import com.sisibibi.api.domain.usersanction.repository.UserSanctionRepository;
import com.sisibibi.api.global.exception.CustomException;
import com.sisibibi.api.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserSanctionPolicyService {

    private static final List<UserSanctionType> SPEECH_AND_STAGE_RESTRICTION_TYPES = List.of(
            UserSanctionType.SPEECH_RESTRICTION,
            UserSanctionType.STAGE_RESTRICTION
    );

    private final UserSanctionRepository userSanctionRepository;

    @Transactional(readOnly = true)
    public void validateChatAllowed(Long userId) {
        validateNotRestricted(
                userId,
                UserSanctionType.CHAT_RESTRICTION,
                ErrorCode.USER_CHAT_RESTRICTED
        );
    }

    @Transactional(readOnly = true)
    public void validateSpeechAllowed(Long userId) {
        validateNotRestrictedIn(
                userId,
                SPEECH_AND_STAGE_RESTRICTION_TYPES,
                ErrorCode.USER_SPEECH_RESTRICTED
        );
    }

    @Transactional(readOnly = true)
    public void validateStageAllowed(Long userId) {
        validateNotRestrictedIn(
                userId,
                SPEECH_AND_STAGE_RESTRICTION_TYPES,
                ErrorCode.USER_STAGE_RESTRICTED
        );
    }

    private void validateNotRestricted(
            Long userId,
            UserSanctionType type,
            ErrorCode errorCode
    ) {
        if (userSanctionRepository.existsActive(userId, type, LocalDateTime.now())) {
            throw new CustomException(errorCode);
        }
    }

    private void validateNotRestrictedIn(
            Long userId,
            List<UserSanctionType> types,
            ErrorCode errorCode
    ) {
        if (userSanctionRepository.existsActiveIn(userId, types, LocalDateTime.now())) {
            throw new CustomException(errorCode);
        }
    }
}
