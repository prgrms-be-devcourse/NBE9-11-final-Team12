package com.sisibibi.api.domain.usersanction.entity;

import com.sisibibi.api.global.exception.CustomException;
import com.sisibibi.api.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserSanctionTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 6, 21, 12, 0);

    @Test
    void create_createsActivePeriodRestriction() {
        UserSanction sanction = UserSanction.create(
                10L,
                99L,
                100L,
                UserSanctionType.CHAT_RESTRICTION,
                "  반복적인 채팅 도배  ",
                NOW,
                NOW.plusHours(24)
        );

        assertThat(sanction.getReason()).isEqualTo("반복적인 채팅 도배");
        assertThat(sanction.isActiveAt(NOW)).isTrue();
        assertThat(sanction.stateAt(NOW)).isEqualTo(UserSanctionState.ACTIVE);
    }

    @Test
    void create_createsRecordedWarningWithoutEndTime() {
        UserSanction sanction = UserSanction.create(
                10L,
                99L,
                null,
                UserSanctionType.WARNING,
                "운영 정책 경고",
                NOW,
                null
        );

        assertThat(sanction.isActiveAt(NOW)).isFalse();
        assertThat(sanction.stateAt(NOW)).isEqualTo(UserSanctionState.RECORDED);
    }

    @Test
    void create_throwsInvalidPeriod_whenRestrictionHasNoEndTime() {
        assertThatThrownBy(() -> UserSanction.create(
                10L,
                99L,
                null,
                UserSanctionType.SPEECH_RESTRICTION,
                "의견 작성 제한",
                NOW,
                null
        ))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USER_SANCTION_INVALID_PERIOD);
    }

    @Test
    void create_throwsInvalidPeriod_whenWarningHasEndTime() {
        assertThatThrownBy(() -> UserSanction.create(
                10L,
                99L,
                null,
                UserSanctionType.WARNING,
                "경고",
                NOW,
                NOW.plusHours(1)
        ))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USER_SANCTION_INVALID_PERIOD);
    }

    @Test
    void create_throwsInvalidPeriod_whenAccountSuspensionHasEndTime() {
        assertThatThrownBy(() -> UserSanction.create(
                10L,
                99L,
                null,
                UserSanctionType.ACCOUNT_SUSPENSION,
                "계정 정지",
                NOW,
                NOW.plusHours(1)
        ))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USER_SANCTION_INVALID_PERIOD);
    }

    @Test
    void create_throwsInvalidPeriod_whenRestrictionEndIsNotAfterStart() {
        assertThatThrownBy(() -> UserSanction.create(
                10L,
                99L,
                null,
                UserSanctionType.CHAT_RESTRICTION,
                "채팅 제한",
                NOW,
                NOW
        ))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USER_SANCTION_INVALID_PERIOD);
    }

    @Test
    void create_allowsAccountSuspensionWithoutEndTime() {
        UserSanction sanction = UserSanction.create(
                10L,
                99L,
                null,
                UserSanctionType.ACCOUNT_SUSPENSION,
                "반복적인 운영 정책 위반",
                NOW,
                null
        );

        assertThat(sanction.isActiveAt(NOW)).isTrue();
        assertThat(sanction.stateAt(NOW)).isEqualTo(UserSanctionState.ACTIVE);
    }

    @Test
    void create_throwsInvalidPeriod_whenRestrictionExceedsThirtyDays() {
        assertThatThrownBy(() -> UserSanction.create(
                10L,
                99L,
                null,
                UserSanctionType.STAGE_RESTRICTION,
                "발언권 제한",
                NOW,
                NOW.plusHours(721)
        ))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USER_SANCTION_INVALID_PERIOD);
    }

    @Test
    void revoke_recordsRevocationInformation() {
        UserSanction sanction = UserSanction.create(
                10L,
                99L,
                null,
                UserSanctionType.CHAT_RESTRICTION,
                "채팅 제한",
                NOW,
                NOW.plusHours(24)
        );

        sanction.revoke(100L, "오인 제재 확인", NOW.plusHours(1));

        assertThat(sanction.stateAt(NOW.plusHours(1))).isEqualTo(UserSanctionState.REVOKED);
        assertThat(sanction.getRevokedBy()).isEqualTo(100L);
        assertThat(sanction.getRevocationReason()).isEqualTo("오인 제재 확인");
    }

    @Test
    void revoke_throwsNotRevocable_whenSanctionIsExpired() {
        UserSanction sanction = UserSanction.create(
                10L,
                99L,
                null,
                UserSanctionType.CHAT_RESTRICTION,
                "채팅 제한",
                NOW,
                NOW.plusHours(1)
        );

        assertThatThrownBy(() -> sanction.revoke(100L, "해제", NOW.plusHours(2)))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USER_SANCTION_NOT_REVOCABLE);
    }

    @Test
    void revoke_throwsReasonRequired_whenReasonIsBlank() {
        UserSanction sanction = UserSanction.create(
                10L,
                99L,
                null,
                UserSanctionType.CHAT_RESTRICTION,
                "채팅 제한",
                NOW,
                NOW.plusHours(24)
        );

        assertThatThrownBy(() -> sanction.revoke(100L, " ", NOW.plusHours(1)))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USER_SANCTION_REASON_REQUIRED);
    }

    @Test
    void extend_updatesEndsAt_whenRequestedEndIsLater() {
        UserSanction sanction = UserSanction.create(
                10L,
                99L,
                null,
                UserSanctionType.SPEECH_RESTRICTION,
                "의견 제한",
                NOW.minusHours(1),
                NOW.plusHours(24)
        );

        sanction.extend(99L, "반복 위반", NOW.plusDays(7), NOW);

        assertThat(sanction.getEndsAt()).isEqualTo(NOW.plusDays(7));
        assertThat(sanction.getExtendedBy()).isEqualTo(99L);
        assertThat(sanction.getExtensionReason()).isEqualTo("반복 위반");
        assertThat(sanction.getExtendedAt()).isEqualTo(NOW);
    }

    @Test
    void extend_throwsNotExtendable_whenRequestedEndIsNotLater() {
        UserSanction sanction = UserSanction.create(
                10L,
                99L,
                null,
                UserSanctionType.SPEECH_RESTRICTION,
                "의견 제한",
                NOW.minusHours(1),
                NOW.plusDays(7)
        );

        assertThatThrownBy(() -> sanction.extend(
                99L,
                "짧은 연장",
                NOW.plusHours(24),
                NOW
        ))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USER_SANCTION_NOT_EXTENDABLE);
    }

    @Test
    void extend_throwsNotExtendable_whenSanctionIsWarning() {
        UserSanction sanction = UserSanction.create(
                10L,
                99L,
                null,
                UserSanctionType.WARNING,
                "경고",
                NOW,
                null
        );

        assertThatThrownBy(() -> sanction.extend(99L, "연장", NOW.plusHours(1), NOW))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USER_SANCTION_NOT_EXTENDABLE);
    }

    @Test
    void extend_throwsNotExtendable_whenSanctionIsAccountSuspension() {
        UserSanction sanction = UserSanction.create(
                10L,
                99L,
                null,
                UserSanctionType.ACCOUNT_SUSPENSION,
                "계정 정지",
                NOW,
                null
        );

        assertThatThrownBy(() -> sanction.extend(99L, "연장", NOW.plusHours(1), NOW))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USER_SANCTION_NOT_EXTENDABLE);
    }

    @Test
    void extend_throwsNotExtendable_whenRequestedEndIsNull() {
        UserSanction sanction = UserSanction.create(
                10L,
                99L,
                null,
                UserSanctionType.SPEECH_RESTRICTION,
                "의견 제한",
                NOW,
                NOW.plusHours(24)
        );

        assertThatThrownBy(() -> sanction.extend(99L, "연장", null, NOW))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USER_SANCTION_NOT_EXTENDABLE);
    }

    @Test
    void extend_throwsNotExtendable_whenRequestedEndExceedsThirtyDaysFromNow() {
        UserSanction sanction = UserSanction.create(
                10L,
                99L,
                null,
                UserSanctionType.SPEECH_RESTRICTION,
                "의견 제한",
                NOW,
                NOW.plusHours(24)
        );

        assertThatThrownBy(() -> sanction.extend(99L, "연장", NOW.plusHours(721), NOW))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USER_SANCTION_NOT_EXTENDABLE);
    }

    @Test
    void stateAt_returnsExpired_whenRestrictionEndIsBeforeNow() {
        UserSanction sanction = UserSanction.create(
                10L,
                99L,
                null,
                UserSanctionType.CHAT_RESTRICTION,
                "채팅 제한",
                NOW,
                NOW.plusHours(1)
        );

        assertThat(sanction.isActiveAt(NOW.plusHours(2))).isFalse();
        assertThat(sanction.stateAt(NOW.plusHours(2))).isEqualTo(UserSanctionState.EXPIRED);
    }

    @Test
    void create_throwsReasonTooLong_whenReasonExceedsLimit() {
        assertThatThrownBy(() -> UserSanction.create(
                10L,
                99L,
                null,
                UserSanctionType.WARNING,
                "가".repeat(501),
                NOW,
                null
        ))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USER_SANCTION_REASON_TOO_LONG);
    }
}
