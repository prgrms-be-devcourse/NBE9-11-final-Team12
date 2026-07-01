package com.sisibibi.api.domain.report.prompt;

import com.sisibibi.api.domain.report.dto.request.AiReportGenerateReq;
import com.sisibibi.api.global.exception.CustomException;
import com.sisibibi.api.global.exception.ErrorCode;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(OutputCaptureExtension.class)
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
    void normalizeAndScan_returnsEmptyList_whenRequestIsNull() {
        List<CustomPromptCommand> result = validator.normalizeAndScan(null);

        assertThat(result).isEmpty();
    }

    @Test
    void normalizeAndScan_trimsAndTruncatesLongLabel() {
        given(promptGuardService.scan("요약 기준 정리"))
                .willReturn(PromptGuardResult.allowed(PromptSeverity.SAFE, null));

        String longLabel = "  1234567890 1234567890 1234567890 1234567890 1234567890 초과  ";

        List<CustomPromptCommand> result = validator.normalizeAndScan(new AiReportGenerateReq(List.of(
                new AiReportGenerateReq.CustomPromptReq(longLabel, "요약 기준 정리")
        )));

        assertThat(result.getFirst().label()).hasSize(50);
        assertThat(result.getFirst().label()).startsWith("1234567890");
    }

    @Test
    void normalizeAndScan_usesDefaultLabel_whenNormalizedLabelIsBlank() {
        given(promptGuardService.scan("요약 기준 정리"))
                .willReturn(PromptGuardResult.allowed(PromptSeverity.SAFE, null));

        List<CustomPromptCommand> result = validator.normalizeAndScan(new AiReportGenerateReq(List.of(
                new AiReportGenerateReq.CustomPromptReq("\u3000", "요약 기준 정리")
        )));

        assertThat(result).containsExactly(new CustomPromptCommand("custom 1", "요약 기준 정리"));
    }

    @Test
    void normalizeAndScan_throwsTooMany_whenPromptCountExceedsLimit() {
        AiReportGenerateReq request = new AiReportGenerateReq(List.of(
                new AiReportGenerateReq.CustomPromptReq("1", "첫 번째"),
                new AiReportGenerateReq.CustomPromptReq("2", "두 번째"),
                new AiReportGenerateReq.CustomPromptReq("3", "세 번째"),
                new AiReportGenerateReq.CustomPromptReq("4", "네 번째")
        ));

        assertThatThrownBy(() -> validator.normalizeAndScan(request))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.AI_REPORT_CUSTOM_PROMPT_TOO_MANY);
    }

    @Test
    void normalizeAndScan_throwsRequired_whenPromptIsNull() {
        AiReportGenerateReq request = new AiReportGenerateReq(List.of(
                new AiReportGenerateReq.CustomPromptReq("빈 요청", null)
        ));

        assertThatThrownBy(() -> validator.normalizeAndScan(request))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.AI_REPORT_CUSTOM_PROMPT_REQUIRED);
    }

    @Test
    void normalizeAndScan_throwsRequired_whenNormalizedPromptIsBlank() {
        AiReportGenerateReq request = new AiReportGenerateReq(List.of(
                new AiReportGenerateReq.CustomPromptReq("빈 요청", "\u3000")
        ));

        assertThatThrownBy(() -> validator.normalizeAndScan(request))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.AI_REPORT_CUSTOM_PROMPT_REQUIRED);
    }

    @Test
    void normalizeAndScan_throwsTooLong_whenPromptExceedsLimit() {
        properties.setCustomPromptMaxLength(5);
        AiReportGenerateReq request = new AiReportGenerateReq(List.of(
                new AiReportGenerateReq.CustomPromptReq("긴 요청", "123456")
        ));

        assertThatThrownBy(() -> validator.normalizeAndScan(request))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.AI_REPORT_CUSTOM_PROMPT_TOO_LONG);
    }

    @Test
    void normalizeAndScan_throwsInvalid_whenPromptHasUnpairedHighSurrogate() {
        AiReportGenerateReq request = new AiReportGenerateReq(List.of(
                new AiReportGenerateReq.CustomPromptReq("잘못된 문자", "요약\uD83D")
        ));

        assertThatThrownBy(() -> validator.normalizeAndScan(request))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.AI_REPORT_CUSTOM_PROMPT_INVALID);
    }

    @Test
    void normalizeAndScan_throwsInvalid_whenPromptHasUnpairedLowSurrogate() {
        AiReportGenerateReq request = new AiReportGenerateReq(List.of(
                new AiReportGenerateReq.CustomPromptReq("잘못된 문자", "요약\uDE00")
        ));

        assertThatThrownBy(() -> validator.normalizeAndScan(request))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.AI_REPORT_CUSTOM_PROMPT_INVALID);
    }

    @Test
    void normalizeAndScan_throwsInvalid_whenPromptHasDisallowedControlCharacter() {
        AiReportGenerateReq request = new AiReportGenerateReq(List.of(
                new AiReportGenerateReq.CustomPromptReq("제어 문자", "요약\u0000정리")
        ));

        assertThatThrownBy(() -> validator.normalizeAndScan(request))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.AI_REPORT_CUSTOM_PROMPT_INVALID);
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
    void normalizeAndScan_blocksMediumSeverityPrompt() {
        given(promptGuardService.scan("의심스러운 개인화 요청"))
                .willReturn(PromptGuardResult.allowed(PromptSeverity.MEDIUM, "role_manipulation"));

        AiReportGenerateReq request = new AiReportGenerateReq(List.of(
                new AiReportGenerateReq.CustomPromptReq(null, "의심스러운 개인화 요청")
        ));

        assertThatThrownBy(() -> validator.normalizeAndScan(request))
                .isInstanceOf(PromptGuardBlockedException.class)
                .extracting("severity")
                .isEqualTo(PromptSeverity.MEDIUM);
    }

    @Test
    void normalizeAndScan_logsUnavailableWhenPromptGuardFailsClosed(CapturedOutput output) {
        given(promptGuardService.scan("요약 관점 정리"))
                .willThrow(new CustomException(ErrorCode.PROMPT_GUARD_UNAVAILABLE));

        AiReportGenerateReq request = new AiReportGenerateReq(List.of(
                new AiReportGenerateReq.CustomPromptReq("관점", "요약 관점 정리")
        ));

        assertThatThrownBy(() -> validator.normalizeAndScan(request))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PROMPT_GUARD_UNAVAILABLE);

        assertThat(output).contains("Prompt guard unavailable. failOpen=false");
        assertThat(output).contains("promptHash=");
    }

    @Test
    void normalizeAndScan_wrapsRuntimeException_whenPromptGuardFailsClosed() {
        doThrow(new IllegalStateException("guard down"))
                .when(promptGuardService)
                .scan("요약 관점 정리");

        AiReportGenerateReq request = new AiReportGenerateReq(List.of(
                new AiReportGenerateReq.CustomPromptReq("관점", "요약 관점 정리")
        ));

        assertThatThrownBy(() -> validator.normalizeAndScan(request))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PROMPT_GUARD_UNAVAILABLE);
    }

    @Test
    void normalizeAndScan_throwsUnavailable_whenPromptGuardReturnsNull() {
        given(promptGuardService.scan("요약 관점 정리"))
                .willReturn(null);

        AiReportGenerateReq request = new AiReportGenerateReq(List.of(
                new AiReportGenerateReq.CustomPromptReq("관점", "요약 관점 정리")
        ));

        assertThatThrownBy(() -> validator.normalizeAndScan(request))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PROMPT_GUARD_UNAVAILABLE);
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
