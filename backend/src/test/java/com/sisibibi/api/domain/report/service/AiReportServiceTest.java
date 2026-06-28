package com.sisibibi.api.domain.report.service;

import com.sisibibi.api.domain.report.dto.event.AiReportGenerationRequestedEvent;
import com.sisibibi.api.domain.report.dto.request.AiReportGenerateReq;
import com.sisibibi.api.domain.report.dto.response.AiReportRes;
import com.sisibibi.api.domain.report.prompt.CustomPromptCommand;
import com.sisibibi.api.domain.report.prompt.CustomPromptValidator;
import com.sisibibi.api.global.exception.CustomException;
import com.sisibibi.api.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AiReportServiceTest {

    @Mock
    private AiReportPersistenceService aiReportPersistenceService;

    @Mock
    private CustomPromptValidator customPromptValidator;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private AiReportService aiReportService;

    @BeforeEach
    void setUp() {
        lenient().when(customPromptValidator.normalizeAndScan(any(AiReportGenerateReq.class)))
                .thenReturn(List.of());
    }

    @Test
    void generateReport_savesRequestedReportAndPublishesEvent_withoutCallingAiServer() {
        AiReportRes requested = requestedResponse();

        given(aiReportPersistenceService.requestGeneration(10L, 7L, List.of()))
                .willReturn(AiReportRequestResult.publish(requested, AiReportGenerationType.BASE_ONLY));

        AiReportRes result = aiReportService.generateReport(10L, 7L, AiReportGenerateReq.empty());

        ArgumentCaptor<AiReportGenerationRequestedEvent> eventCaptor =
                ArgumentCaptor.forClass(AiReportGenerationRequestedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(result.status()).isEqualTo("REQUESTED");
        assertThat(eventCaptor.getValue().reportId()).isEqualTo(55L);
        assertThat(eventCaptor.getValue().roomId()).isEqualTo(10L);
        assertThat(eventCaptor.getValue().generationType()).isEqualTo(AiReportGenerationType.BASE_ONLY);
    }

    @Test
    void generateReport_returnsExistingReportWithoutPublishingEvent_whenGenerationIsAlreadyInProgress() {
        AiReportRes requested = requestedResponse();

        given(aiReportPersistenceService.requestGeneration(10L, 7L, List.of()))
                .willReturn(AiReportRequestResult.skip(requested));

        AiReportRes result = aiReportService.generateReport(10L, 7L, AiReportGenerateReq.empty());

        assertThat(result.status()).isEqualTo("REQUESTED");
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void generateReport_passesAuthenticatedUserIdForUserScopedCustomReports() {
        AiReportGenerateReq request = customPromptRequest("minority view");
        List<CustomPromptCommand> normalizedPrompts = List.of(new CustomPromptCommand("custom 1", "minority view"));
        AiReportRes requested = requestedResponse();

        given(customPromptValidator.normalizeAndScan(request)).willReturn(normalizedPrompts);
        given(aiReportPersistenceService.requestGeneration(10L, 7L, normalizedPrompts))
                .willReturn(AiReportRequestResult.publish(requested, AiReportGenerationType.BASE_WITH_CUSTOM));

        AiReportRes result = aiReportService.generateReport(10L, 7L, request);

        assertThat(result.status()).isEqualTo("REQUESTED");
        verify(aiReportPersistenceService).requestGeneration(10L, 7L, normalizedPrompts);
        verify(eventPublisher).publishEvent(any(AiReportGenerationRequestedEvent.class));
    }

    @Test
    void generateReport_rejectsInvalidCustomPromptBeforeSavingOrPublishing() {
        AiReportGenerateReq request = customPromptRequest(" ");
        given(customPromptValidator.normalizeAndScan(request))
                .willThrow(new CustomException(ErrorCode.AI_REPORT_CUSTOM_PROMPT_REQUIRED));

        assertThatThrownBy(() -> aiReportService.generateReport(10L, 7L, request))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.AI_REPORT_CUSTOM_PROMPT_REQUIRED);

        verify(aiReportPersistenceService, never()).requestGeneration(
                org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.<List<CustomPromptCommand>>any()
        );
        verify(eventPublisher, never()).publishEvent(any());
    }

    private AiReportRes requestedResponse() {
        return new AiReportRes(
                55L,
                10L,
                "REQUESTED",
                null,
                List.of(),
                null,
                null,
                null,
                null,
                null,
                null
        );
    }

    private AiReportGenerateReq customPromptRequest(String... prompts) {
        return new AiReportGenerateReq(
                java.util.Arrays.stream(prompts)
                        .map(prompt -> new AiReportGenerateReq.CustomPromptReq(null, prompt))
                        .toList()
        );
    }
}
