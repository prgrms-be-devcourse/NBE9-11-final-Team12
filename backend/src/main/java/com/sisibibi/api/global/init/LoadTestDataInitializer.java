package com.sisibibi.api.global.init;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@Profile("load-test")
@RequiredArgsConstructor
public class LoadTestDataInitializer implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Value("${app.load-test.seed.enabled:true}")
    private boolean enabled;

    @Value("${app.load-test.seed.room-id:1}")
    private long roomId;

    @Value("${app.load-test.seed.user-id-start:1}")
    private long userIdStart;

    @Value("${app.load-test.seed.user-count:1000}")
    private int userCount;

    @Value("${app.load-test.seed.reset-speaking-queue:true}")
    private boolean resetSpeakingQueue;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!enabled) {
            log.info("[LOAD TEST SEED] skipped");
            return;
        }

        if (resetSpeakingQueue) {
            jdbcTemplate.update(
                    "delete from speaking_queue where room_id = ?",
                    roomId
            );
        }

        jdbcTemplate.update(
                """
                        insert into rooms (id, status)
                        values (?, 'OPEN')
                        on duplicate key update status = 'OPEN'
                        """,
                roomId
        );

        for (long userId = userIdStart; userId < userIdStart + userCount; userId++) {
            jdbcTemplate.update(
                    """
                            insert into users (id, status)
                            values (?, 'ACTIVE')
                            on duplicate key update status = 'ACTIVE'
                            """,
                    userId
            );
        }

        log.info(
                "[LOAD TEST SEED] roomId={}, userIdStart={}, userCount={}, resetSpeakingQueue={}",
                roomId,
                userIdStart,
                userCount,
                resetSpeakingQueue
        );
    }
}
