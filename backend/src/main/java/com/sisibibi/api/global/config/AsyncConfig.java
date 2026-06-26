package com.sisibibi.api.global.config;

import java.util.concurrent.Executor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync
public class AsyncConfig {

    public static final String DOMAIN_EVENT_TASK_EXECUTOR = "domainEventTaskExecutor";

    @Bean(name = DOMAIN_EVENT_TASK_EXECUTOR)
    public Executor domainEventTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("domain-event-");
        executor.initialize();
        return executor;
    }
}
