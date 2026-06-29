package com.sisibibi.api.domain.report.service;

public interface AiReportPdfRenderer {

    byte[] render(AiReportPdfModel model);
}
