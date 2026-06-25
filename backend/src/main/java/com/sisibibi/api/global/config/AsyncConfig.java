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
    public static final String AI_COUNTER_ISSUE_TASK_EXECUTOR = "aiCounterIssueTaskExecutor";

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

    @Bean(name = AI_COUNTER_ISSUE_TASK_EXECUTOR)
    public Executor aiCounterIssueTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("ai-counter-issue-");
        executor.initialize();
        return executor;
    }
}
