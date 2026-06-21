package com.sisibibi.api.domain.usersanction.service;

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
public class UserSanctionPolicyService {

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
        validateNotRestricted(
                userId,
                UserSanctionType.SPEECH_RESTRICTION,
                ErrorCode.USER_SPEECH_RESTRICTED
        );
    }

    @Transactional(readOnly = true)
    public void validateStageAllowed(Long userId) {
        validateNotRestricted(
                userId,
                UserSanctionType.STAGE_RESTRICTION,
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
}
