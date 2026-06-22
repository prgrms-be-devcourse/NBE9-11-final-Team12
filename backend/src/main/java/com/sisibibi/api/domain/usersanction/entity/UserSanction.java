package com.sisibibi.api.domain.usersanction.entity;

import com.sisibibi.api.global.exception.CustomException;
import com.sisibibi.api.global.exception.ErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.time.Duration;

@Getter
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(
        name = "user_sanctions",
        indexes = {
                @Index(
                        name = "idx_user_sanctions_user_type_end",
                        columnList = "user_id, type, ends_at"
                ),
                @Index(
                        name = "idx_user_sanctions_report_id",
                        columnList = "report_id"
                )
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserSanction {

    private static final int MAX_REASON_LENGTH = 500;
    private static final long MAX_RESTRICTION_HOURS = 720;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "admin_user_id", nullable = false)
    private Long adminUserId;

    @Column(name = "report_id")
    private Long reportId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private UserSanctionType type;

    @Column(nullable = false, length = 500)
    private String reason;

    @Column(name = "starts_at", nullable = false)
    private LocalDateTime startsAt;

    @Column(name = "ends_at")
    private LocalDateTime endsAt;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    @Column(name = "revoked_by")
    private Long revokedBy;

    @Column(name = "revocation_reason", length = 500)
    private String revocationReason;

    @Column(name = "extended_at")
    private LocalDateTime extendedAt;

    @Column(name = "extended_by")
    private Long extendedBy;

    @Column(name = "extension_reason", length = 500)
    private String extensionReason;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private UserSanction(
            Long userId,
            Long adminUserId,
            Long reportId,
            UserSanctionType type,
            String reason,
            LocalDateTime startsAt,
            LocalDateTime endsAt
    ) {
        this.userId = userId;
        this.adminUserId = adminUserId;
        this.reportId = reportId;
        this.type = type;
        this.reason = normalizeRequiredReason(reason);
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        validatePeriod(type, startsAt, endsAt);
    }

    public static UserSanction create(
            Long userId,
            Long adminUserId,
            Long reportId,
            UserSanctionType type,
            String reason,
            LocalDateTime startsAt,
            LocalDateTime endsAt
    ) {
        return new UserSanction(
                userId,
                adminUserId,
                reportId,
                type,
                reason,
                startsAt,
                endsAt
        );
    }

    public boolean isActiveAt(LocalDateTime now) {
        return revokedAt == null
                && type != UserSanctionType.WARNING
                && !startsAt.isAfter(now)
                && endsAt.isAfter(now);
    }

    public UserSanctionState stateAt(LocalDateTime now) {
        if (revokedAt != null) {
            return UserSanctionState.REVOKED;
        }
        if (type == UserSanctionType.WARNING) {
            return UserSanctionState.RECORDED;
        }
        if (!endsAt.isAfter(now)) {
            return UserSanctionState.EXPIRED;
        }
        return UserSanctionState.ACTIVE;
    }

    public void revoke(Long adminUserId, String reason, LocalDateTime now) {
        if (!isActiveAt(now)) {
            throw new CustomException(ErrorCode.USER_SANCTION_NOT_REVOCABLE);
        }

        revokedAt = now;
        revokedBy = adminUserId;
        revocationReason = normalizeRequiredReason(reason);
    }

    public void extend(
            Long adminUserId,
            String reason,
            LocalDateTime requestedEndsAt,
            LocalDateTime now
    ) {
        if (!isActiveAt(now)
                || requestedEndsAt == null
                || !requestedEndsAt.isAfter(endsAt)
                || Duration.between(now, requestedEndsAt).toHours() > MAX_RESTRICTION_HOURS) {
            throw new CustomException(ErrorCode.USER_SANCTION_NOT_EXTENDABLE);
        }

        endsAt = requestedEndsAt;
        extendedAt = now;
        extendedBy = adminUserId;
        extensionReason = normalizeRequiredReason(reason);
    }

    private void validatePeriod(
            UserSanctionType type,
            LocalDateTime startsAt,
            LocalDateTime endsAt
    ) {
        if (type == UserSanctionType.WARNING) {
            if (endsAt != null) {
                throw new CustomException(ErrorCode.USER_SANCTION_INVALID_PERIOD);
            }
            return;
        }

        if (endsAt == null
                || !endsAt.isAfter(startsAt)
                || Duration.between(startsAt, endsAt).toHours() > MAX_RESTRICTION_HOURS) {
            throw new CustomException(ErrorCode.USER_SANCTION_INVALID_PERIOD);
        }
    }

    private String normalizeRequiredReason(String reason) {
        if (reason == null || reason.isBlank()) {
            throw new CustomException(ErrorCode.USER_SANCTION_REASON_REQUIRED);
        }

        String normalizedReason = reason.trim();
        if (normalizedReason.length() > MAX_REASON_LENGTH) {
            throw new CustomException(ErrorCode.USER_SANCTION_REASON_TOO_LONG);
        }
        return normalizedReason;
    }
}
