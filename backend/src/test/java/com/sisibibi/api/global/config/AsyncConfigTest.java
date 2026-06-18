package com.sisibibi.api.global.config;

import java.util.concurrent.Executor;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import static org.assertj.core.api.Assertions.assertThat;

class AsyncConfigTest {

    @Test
    void domainEventTaskExecutor_createsExecutorForDomainEvents() {
        AsyncConfig config = new AsyncConfig();

        Executor executor = config.domainEventTaskExecutor();

        assertThat(executor).isInstanceOf(ThreadPoolTaskExecutor.class);
        ThreadPoolTaskExecutor taskExecutor = (ThreadPoolTaskExecutor) executor;
        assertThat(taskExecutor.getCorePoolSize()).isEqualTo(2);
        assertThat(taskExecutor.getMaxPoolSize()).isEqualTo(4);
        taskExecutor.shutdown();
    }
}
