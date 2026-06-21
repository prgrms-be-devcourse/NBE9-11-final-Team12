package com.sisibibi.api.domain.usersanction.repository;

import com.sisibibi.api.domain.usersanction.entity.UserSanction;
import com.sisibibi.api.domain.usersanction.entity.UserSanctionType;
import com.sisibibi.api.global.config.JpaAuditingConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@Import(JpaAuditingConfig.class)
class UserSanctionRepositoryTest {

    @Autowired
    private UserSanctionRepository userSanctionRepository;

    @Test
    void existsActive_returnsTrueOnlyWithinActivePeriod() {
        LocalDateTime now = LocalDateTime.of(2026, 6, 21, 12, 0);
        userSanctionRepository.saveAndFlush(UserSanction.create(
                10L,
                99L,
                null,
                UserSanctionType.CHAT_RESTRICTION,
                "채팅 제한",
                now.minusHours(1),
                now.plusHours(1)
        ));

        assertThat(userSanctionRepository.existsActive(
                10L,
                UserSanctionType.CHAT_RESTRICTION,
                now
        )).isTrue();
        assertThat(userSanctionRepository.existsActive(
                10L,
                UserSanctionType.CHAT_RESTRICTION,
                now.plusHours(2)
        )).isFalse();
    }

    @Test
    void findByUserId_returnsLatestHistoryFirst() {
        LocalDateTime now = LocalDateTime.of(2026, 6, 21, 12, 0);
        userSanctionRepository.saveAndFlush(UserSanction.create(
                10L,
                99L,
                null,
                UserSanctionType.WARNING,
                "첫 번째 경고",
                now,
                null
        ));
        UserSanction latest = userSanctionRepository.saveAndFlush(UserSanction.create(
                10L,
                99L,
                null,
                UserSanctionType.WARNING,
                "두 번째 경고",
                now.plusMinutes(1),
                null
        ));

        assertThat(userSanctionRepository.findByUserIdOrderByCreatedAtDescIdDesc(
                10L,
                PageRequest.of(0, 20)
        ).getContent().getFirst().getId()).isEqualTo(latest.getId());
    }
}
