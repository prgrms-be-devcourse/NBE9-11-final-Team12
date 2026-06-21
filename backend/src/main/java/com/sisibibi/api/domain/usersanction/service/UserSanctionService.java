package com.sisibibi.api.domain.usersanction.service;

import com.sisibibi.api.domain.speechreport.entity.SpeechReport;
import com.sisibibi.api.domain.speechreport.entity.SpeechReportStatus;
import com.sisibibi.api.domain.speechreport.repository.SpeechReportRepository;
import com.sisibibi.api.domain.user.repository.UserRepository;
import com.sisibibi.api.domain.user.entity.User;
import com.sisibibi.api.domain.user.entity.UserRole;
import com.sisibibi.api.domain.usersanction.dto.request.UserSanctionCreateReq;
import com.sisibibi.api.domain.usersanction.dto.response.UserSanctionRes;
import com.sisibibi.api.domain.usersanction.entity.UserSanction;
import com.sisibibi.api.domain.usersanction.entity.UserSanctionType;
import com.sisibibi.api.domain.usersanction.repository.UserSanctionRepository;
import com.sisibibi.api.global.exception.CustomException;
import com.sisibibi.api.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserSanctionService {

    private final UserRepository userRepository;
    private final SpeechReportRepository speechReportRepository;
    private final UserSanctionRepository userSanctionRepository;

    @Transactional
    public UserSanctionRes createSanction(
            Long userId,
            Long adminUserId,
            UserSanctionCreateReq request
    ) {
        User targetUser = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        if (targetUser.getRole() == UserRole.ADMIN) {
            throw new CustomException(ErrorCode.USER_SANCTION_ADMIN_NOT_ALLOWED);
        }

        validateReport(userId, request.reportId());

        LocalDateTime startsAt = LocalDateTime.now();
        LocalDateTime endsAt = calculateEndsAt(request.type(), request.durationHours(), startsAt);
        if (request.type() != UserSanctionType.WARNING
                && userSanctionRepository.existsActive(
                userId,
                request.type(),
                startsAt
        )) {
            throw new CustomException(ErrorCode.USER_SANCTION_ALREADY_ACTIVE);
        }

        UserSanction sanction = UserSanction.create(
                userId,
                adminUserId,
                request.reportId(),
                request.type(),
                request.reason(),
                startsAt,
                endsAt
        );
        UserSanction savedSanction = userSanctionRepository.save(sanction);
        log.info(
                "User sanction created. sanctionId={}, userId={}, adminUserId={}, type={}, reportId={}",
                savedSanction.getId(),
                userId,
                adminUserId,
                request.type(),
                request.reportId()
        );

        return UserSanctionRes.from(savedSanction, startsAt);
    }

    @Transactional(readOnly = true)
    public Page<UserSanctionRes> getSanctions(Long userId, Pageable pageable) {
        if (!userRepository.existsById(userId)) {
            throw new CustomException(ErrorCode.USER_NOT_FOUND);
        }

        LocalDateTime now = LocalDateTime.now();
        return userSanctionRepository
                .findByUserIdOrderByCreatedAtDescIdDesc(userId, pageable)
                .map(sanction -> UserSanctionRes.from(sanction, now));
    }

    @Transactional
    public UserSanctionRes revokeSanction(
            Long userId,
            Long sanctionId,
            Long adminUserId,
            String reason
    ) {
        UserSanction sanction = userSanctionRepository
                .findByIdAndUserIdForUpdate(sanctionId, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_SANCTION_NOT_FOUND));

        LocalDateTime now = LocalDateTime.now();
        sanction.revoke(adminUserId, reason, now);
        log.info(
                "User sanction revoked. sanctionId={}, userId={}, adminUserId={}, type={}",
                sanctionId,
                userId,
                adminUserId,
                sanction.getType()
        );

        return UserSanctionRes.from(sanction, now);
    }

    private void validateReport(Long userId, Long reportId) {
        if (reportId == null) {
            return;
        }

        SpeechReport report = speechReportRepository.findById(reportId)
                .orElseThrow(() -> new CustomException(ErrorCode.SPEECH_REPORT_NOT_FOUND));
        if (report.getStatus() != SpeechReportStatus.RESOLVED) {
            throw new CustomException(ErrorCode.USER_SANCTION_REPORT_NOT_RESOLVED);
        }
        if (!report.getReportedUserId().equals(userId)) {
            throw new CustomException(ErrorCode.USER_SANCTION_REPORT_MISMATCH);
        }
    }

    private LocalDateTime calculateEndsAt(
            UserSanctionType type,
            Integer durationHours,
            LocalDateTime startsAt
    ) {
        if (type == UserSanctionType.WARNING) {
            if (durationHours != null) {
                throw new CustomException(ErrorCode.USER_SANCTION_INVALID_PERIOD);
            }
            return null;
        }
        if (durationHours == null) {
            throw new CustomException(ErrorCode.USER_SANCTION_INVALID_PERIOD);
        }
        return startsAt.plusHours(durationHours);
    }
}
