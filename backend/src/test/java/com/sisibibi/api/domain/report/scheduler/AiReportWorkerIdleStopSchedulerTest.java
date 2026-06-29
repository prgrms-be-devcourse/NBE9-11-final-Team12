package com.sisibibi.api.domain.report.scheduler;

import com.sisibibi.api.domain.report.worker.AiReportWorkerEc2Properties;
import com.sisibibi.api.domain.report.worker.AiReportWorkerEc2Service;
import com.sisibibi.api.domain.report.worker.AiReportWorkerQueueMonitor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class AiReportWorkerIdleStopSchedulerTest {

    private final AiReportWorkerQueueMonitor queueMonitor = mock(AiReportWorkerQueueMonitor.class);
    private final AiReportWorkerEc2Service ec2Service = mock(AiReportWorkerEc2Service.class);
    private final AiReportWorkerEc2Properties properties = new AiReportWorkerEc2Properties();
    private final Clock clock = Clock.fixed(
            Instant.parse("2026-06-29T12:00:00Z"),
            ZoneId.of("Asia/Seoul")
    );

    private AiReportWorkerIdleStopScheduler scheduler;

    @BeforeEach
    void setUp() {
        properties.setEnabled(true);
        properties.setIdleTimeout(Duration.ZERO);
        scheduler = new AiReportWorkerIdleStopScheduler(queueMonitor, ec2Service, properties, clock);
    }

    @Test
    void stopWorkerWhenQueueIsIdle_stopsAfterIdleTimeout() {
        given(queueMonitor.getQueueDepth())
                .willReturn(new AiReportWorkerQueueMonitor.QueueDepth(0, 0));

        scheduler.stopWorkerWhenQueueIsIdle();
        verifyNoInteractions(ec2Service);

        scheduler.stopWorkerWhenQueueIsIdle();
        verify(ec2Service).stopWorkerIfEnabled();
    }

    @Test
    void stopWorkerWhenQueueIsIdle_doesNotStopWhenQueueHasMessages() {
        given(queueMonitor.getQueueDepth())
                .willReturn(new AiReportWorkerQueueMonitor.QueueDepth(1, 0));

        scheduler.stopWorkerWhenQueueIsIdle();

        verifyNoInteractions(ec2Service);
    }

    @Test
    void stopWorkerWhenQueueIsIdle_skipsWhenEc2ControlIsDisabled() {
        properties.setEnabled(false);

        scheduler.stopWorkerWhenQueueIsIdle();

        verifyNoInteractions(queueMonitor, ec2Service);
    }
}
