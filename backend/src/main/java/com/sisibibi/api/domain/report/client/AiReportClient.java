package com.sisibibi.api.domain.report.client;

import com.sisibibi.api.domain.report.client.dto.AiReportGenerateReq;
import com.sisibibi.api.domain.report.client.dto.AiReportGenerateRes;

public interface AiReportClient {

    AiReportGenerateRes generate(AiReportGenerateReq request);
}
