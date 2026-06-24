package com.sisibibi.api.domain.report.scheduler;

import com.sisibibi.api.domain.report.service.AiReportPublishRetryService;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class AiReportPublishRetrySchedulerTest {

    private final AiReportPublishRetryService aiReportPublishRetryService = mock(AiReportPublishRetryService.class);
    private final AiReportPublishRetryScheduler scheduler =
            new AiReportPublishRetryScheduler(aiReportPublishRetryService);

    @Test
    void republishStaleRequests_delegatesToService() {
        scheduler.republishStaleRequests();

        verify(aiReportPublishRetryService).republishStaleRequests();
    }

    @Test
    void republishStaleRequests_doesNotPropagateServiceException() {
        doThrow(new IllegalStateException("retry failed"))
                .when(aiReportPublishRetryService)
                .republishStaleRequests();

        scheduler.republishStaleRequests();

        verify(aiReportPublishRetryService).republishStaleRequests();
    }
}
