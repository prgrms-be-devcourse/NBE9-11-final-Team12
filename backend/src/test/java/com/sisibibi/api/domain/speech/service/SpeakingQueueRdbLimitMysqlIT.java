package com.sisibibi.api.domain.speech.service;

import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("rdb-limit-mysql")
@EnabledIfSystemProperty(named = "rdb.limit.mysql.enabled", matches = "true")
class SpeakingQueueRdbLimitMysqlIT extends SpeakingQueueRdbLimitTestSupport {
}
