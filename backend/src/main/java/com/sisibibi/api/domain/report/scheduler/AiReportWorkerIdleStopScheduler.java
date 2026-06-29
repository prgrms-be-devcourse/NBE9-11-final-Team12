package com.sisibibi.api.domain.report.scheduler;

import com.sisibibi.api.domain.report.worker.AiReportWorkerEc2Properties;
import com.sisibibi.api.domain.report.worker.AiReportWorkerEc2Service;
import com.sisibibi.api.domain.report.worker.AiReportWorkerQueueMonitor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "app.ai-report.worker.ec2",
        name = "idle-stop-enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class AiReportWorkerIdleStopScheduler {

    private final AiReportWorkerQueueMonitor queueMonitor;
    private final AiReportWorkerEc2Service ec2Service;
    private final AiReportWorkerEc2Properties properties;
    private final Clock clock;

    private LocalDateTime idleStartedAt;

    @Scheduled(fixedDelayString = "${app.ai-report.worker.ec2.idle-check-fixed-delay-ms:60000}")
    @SchedulerLock(
            name = "aiReportWorkerIdleStopScheduler",
            lockAtMostFor = "PT5M",
            lockAtLeastFor = "PT30S"
    )
    public void stopWorkerWhenQueueIsIdle() {
        if (!properties.isEnabled()) {
            idleStartedAt = null;
            return;
        }

        try {
            AiReportWorkerQueueMonitor.QueueDepth queueDepth = queueMonitor.getQueueDepth();
            if (!queueDepth.isIdle()) {
                idleStartedAt = null;
                ec2Service.startWorkerIfEnabled();
                log.debug("AI report queue is not idle. visible={}, notVisible={}",
                        queueDepth.visible(),
                        queueDepth.notVisible());
                return;
            }

            LocalDateTime now = LocalDateTime.now(clock);
            if (idleStartedAt == null) {
                idleStartedAt = now;
                log.debug("AI report queue became idle. idleStartedAt={}", idleStartedAt);
                return;
            }

            if (idleStartedAt.plus(properties.getIdleTimeout()).isAfter(now)) {
                return;
            }

            ec2Service.stopWorkerIfEnabled();
            idleStartedAt = null;
        } catch (RuntimeException e) {
            log.warn("Failed to evaluate AI Worker EC2 idle stop.", e);
        }
    }
}
