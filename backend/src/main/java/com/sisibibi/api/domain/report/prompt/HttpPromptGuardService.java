package com.sisibibi.api.domain.report.prompt;

import com.sisibibi.api.global.exception.CustomException;
import com.sisibibi.api.global.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.net.http.HttpClient;
import java.util.List;

@Slf4j
@Component
public class HttpPromptGuardService implements PromptGuardService {

    private final RestClient restClient;
    private final PromptGuardProperties properties;

    public HttpPromptGuardService(RestClient.Builder restClientBuilder, PromptGuardProperties properties) {
        this.properties = properties;
        this.restClient = StringUtils.hasText(properties.getBaseUrl())
                ? restClientBuilder
                .baseUrl(properties.getBaseUrl())
                .requestFactory(createRequestFactory(properties))
                .build()
                : null;
    }

    @Override
    public PromptGuardResult scan(String content) {
        if (restClient == null) {
            throw new CustomException(ErrorCode.PROMPT_GUARD_UNAVAILABLE);
        }

        try {
            PromptGuardScanRes response = restClient.post()
                    .uri(properties.getScanPath())
                    .body(new PromptGuardScanReq(content, "analyze"))
                    .retrieve()
                    .body(PromptGuardScanRes.class);

            if (response == null) {
                throw new CustomException(ErrorCode.PROMPT_GUARD_UNAVAILABLE);
            }

            return response.toResult();
        } catch (CustomException e) {
            throw e;
        } catch (RestClientResponseException e) {
            log.warn("Prompt guard request failed. status={}", e.getStatusCode());
            throw new CustomException(ErrorCode.PROMPT_GUARD_UNAVAILABLE);
        } catch (RestClientException e) {
            log.warn("Prompt guard request failed.", e);
            throw new CustomException(ErrorCode.PROMPT_GUARD_UNAVAILABLE);
        }
    }

    private JdkClientHttpRequestFactory createRequestFactory(PromptGuardProperties properties) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.getConnectTimeout())
                .build();

        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(properties.getReadTimeout());
        return requestFactory;
    }

    private record PromptGuardScanReq(
            String content,
            String type
    ) {
    }

    private record PromptGuardScanRes(
            Boolean blocked,
            String action,
            List<PromptGuardMatch> matches
    ) {

        private PromptGuardResult toResult() {
            PromptSeverity maxSeverity = maxSeverity();
            boolean policyBlock = Boolean.TRUE.equals(blocked)
                    || isBlockAction(action)
                    || hasBlockingMatchAction();
            boolean shouldBlock = policyBlock || maxSeverity.blocksRequest();
            PromptSeverity effectiveSeverity = policyBlock && maxSeverity.rank() < PromptSeverity.HIGH.rank()
                    ? PromptSeverity.HIGH
                    : maxSeverity;
            String reason = matches == null || matches.isEmpty() ? action : matches.get(0).reason();

            return new PromptGuardResult(shouldBlock, effectiveSeverity, reason);
        }

        private PromptSeverity maxSeverity() {
            if (matches == null || matches.isEmpty()) {
                return PromptSeverity.SAFE;
            }

            PromptSeverity max = PromptSeverity.SAFE;
            for (PromptGuardMatch match : matches) {
                PromptSeverity severity = PromptSeverity.from(match.severity());
                if (severity.rank() > max.rank()) {
                    max = severity;
                }
            }
            return max;
        }

        private boolean hasBlockingMatchAction() {
            if (matches == null) {
                return false;
            }

            return matches.stream().anyMatch(match -> isBlockAction(match.action()));
        }

        private boolean isBlockAction(String action) {
            if (action == null) {
                return false;
            }

            String normalized = action.trim().toLowerCase();
            return normalized.equals("block")
                    || normalized.equals("blocked")
                    || normalized.equals("deny")
                    || normalized.equals("denied");
        }
    }

    private record PromptGuardMatch(
            String severity,
            String action,
            String reason
    ) {
    }
}
