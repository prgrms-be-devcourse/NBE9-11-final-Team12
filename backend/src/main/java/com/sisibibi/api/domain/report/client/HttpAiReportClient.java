package com.sisibibi.api.domain.report.client;

import com.sisibibi.api.domain.report.client.dto.AiReportGenerateReq;
import com.sisibibi.api.domain.report.client.dto.AiReportGenerateRes;
import com.sisibibi.api.global.exception.CustomException;
import com.sisibibi.api.global.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Slf4j
@Component
public class HttpAiReportClient implements AiReportClient {

    private final RestClient restClient;
    private final AiReportProperties properties;

    public HttpAiReportClient(RestClient.Builder restClientBuilder, AiReportProperties properties) {
        validateBaseUrl(properties);

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        int timeoutMillis = Math.toIntExact(properties.getTimeout().toMillis());
        requestFactory.setConnectTimeout(timeoutMillis);
        requestFactory.setReadTimeout(timeoutMillis);

        this.restClient = restClientBuilder
                .baseUrl(properties.getBaseUrl())
                .requestFactory(requestFactory)
                .build();
        this.properties = properties;
    }

    @Override
    public AiReportGenerateRes generate(AiReportGenerateReq request) {
        try {
            AiReportGenerateRes response = restClient.post()
                    .uri(properties.getGeneratePath())
                    .body(request)
                    .retrieve()
                    .body(AiReportGenerateRes.class);

            if (response == null) {
                throw new CustomException(ErrorCode.AI_REPORT_INVALID_RESPONSE);
            }

            return response;
        } catch (CustomException e) {
            throw e;
        } catch (RestClientResponseException e) {
            log.warn("AI report request failed. status={}", e.getStatusCode());
            throw new CustomException(ErrorCode.AI_REPORT_GENERATE_FAILED);
        } catch (RestClientException e) {
            log.warn("AI report request failed.", e);
            throw new CustomException(ErrorCode.AI_REPORT_GENERATE_FAILED);
        }
    }

    private void validateBaseUrl(AiReportProperties properties) {
        if (!StringUtils.hasText(properties.getBaseUrl())) {
            throw new CustomException(ErrorCode.AI_REPORT_CONFIG_MISSING);
        }
    }
}
