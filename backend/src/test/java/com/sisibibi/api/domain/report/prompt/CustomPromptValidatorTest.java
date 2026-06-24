package com.sisibibi.api.domain.report.prompt;

import com.sisibibi.api.domain.report.dto.request.AiReportGenerateReq;
import com.sisibibi.api.global.exception.CustomException;
import com.sisibibi.api.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class CustomPromptValidatorTest {

    private PromptGuardService promptGuardService;
    private PromptGuardProperties properties;
    private CustomPromptValidator validator;

    @BeforeEach
    void setUp() {
        promptGuardService = mock(PromptGuardService.class);
        properties = new PromptGuardProperties();
        validator = new CustomPromptValidator(promptGuardService, properties);
    }

    @Test
    void normalizeAndScan_returnsNormalizedPromptsWithDefaultLabels() {
        given(promptGuardService.scan("핵심 쟁점 정리"))
                .willReturn(PromptGuardResult.allowed(PromptSeverity.SAFE, null));

        List<CustomPromptCommand> result = validator.normalizeAndScan(new AiReportGenerateReq(List.of(
                new AiReportGenerateReq.CustomPromptReq(null, "  핵심\t쟁점\n정리  ")
        )));

        assertThat(result).containsExactly(new CustomPromptCommand("custom 1", "핵심 쟁점 정리"));
        verify(promptGuardService).scan("핵심 쟁점 정리");
    }

    @Test
    void normalizeAndScan_stopsAtFirstBlockedPrompt() {
        given(promptGuardService.scan("첫 번째 요청"))
                .willReturn(PromptGuardResult.allowed(PromptSeverity.SAFE, null));
        given(promptGuardService.scan("위험한 요청"))
                .willReturn(PromptGuardResult.blocked(PromptSeverity.HIGH, "policy"));

        AiReportGenerateReq request = new AiReportGenerateReq(List.of(
                new AiReportGenerateReq.CustomPromptReq(null, "첫 번째 요청"),
                new AiReportGenerateReq.CustomPromptReq(null, "위험한 요청"),
                new AiReportGenerateReq.CustomPromptReq(null, "세 번째 요청")
        ));

        assertThatThrownBy(() -> validator.normalizeAndScan(request))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PROMPT_GUARD_BLOCKED);

        verify(promptGuardService).scan("첫 번째 요청");
        verify(promptGuardService).scan("위험한 요청");
        verify(promptGuardService, never()).scan("세 번째 요청");
    }

    @Test
    void normalizeAndScan_allowsWhenPromptGuardFailsWithFailOpenPolicy() {
        properties.setFailOpen(true);
        given(promptGuardService.scan("요약 관점 정리"))
                .willThrow(new CustomException(ErrorCode.PROMPT_GUARD_UNAVAILABLE));

        List<CustomPromptCommand> result = validator.normalizeAndScan(new AiReportGenerateReq(List.of(
                new AiReportGenerateReq.CustomPromptReq("관점", "요약 관점 정리")
        )));

        assertThat(result).containsExactly(new CustomPromptCommand("관점", "요약 관점 정리"));
    }
}
