package com.sisibibi.api.domain.report.service;

public interface AiReportNotificationSender {

    void sendPdfReady(AiReportPdfReadyCommand command);
}
