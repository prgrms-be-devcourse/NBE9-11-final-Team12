package com.sisibibi.api.domain.speechreport.service;

import com.sisibibi.api.domain.speech.entity.Speech;
import com.sisibibi.api.domain.speech.entity.SpeechStance;
import com.sisibibi.api.domain.speech.repository.SpeechRepository;
import com.sisibibi.api.domain.speechreport.dto.command.SpeechReportCreateCommand;
import com.sisibibi.api.domain.speechreport.dto.response.SpeechReportCreateRes;
import com.sisibibi.api.domain.speechreport.entity.SpeechReport;
import com.sisibibi.api.domain.speechreport.entity.SpeechReportReason;
import com.sisibibi.api.domain.speechreport.entity.SpeechReportStatus;
import com.sisibibi.api.domain.speechreport.repository.SpeechReportRepository;
import com.sisibibi.api.global.exception.CustomException;
import com.sisibibi.api.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SpeechReportServiceTest {

    @Mock
    private SpeechRepository speechRepository;

    @Mock
    private SpeechReportRepository speechReportRepository;

    @InjectMocks
    private SpeechReportService speechReportService;

    @Test
    void createReport_savesPendingReport() {
        Speech speech = Speech.createMainOpinion(1L, 30L, "신고 대상 의견", SpeechStance.PRO);
        given(speechRepository.findByIdAndDeletedFalse(10L)).willReturn(Optional.of(speech));
        given(speechReportRepository.existsBySpeechIdAndReporterUserId(10L, 20L))
                .willReturn(false);
        given(speechReportRepository.save(org.mockito.ArgumentMatchers.any(SpeechReport.class)))
                .willAnswer(invocation -> {
                    SpeechReport report = invocation.getArgument(0);
                    ReflectionTestUtils.setField(report, "id", 100L);
                    ReflectionTestUtils.setField(report, "createdAt", LocalDateTime.of(2026, 6, 14, 12, 0));
                    return report;
                });

        SpeechReportCreateRes response = speechReportService.createReport(
                10L,
                20L,
                new SpeechReportCreateCommand(
                        SpeechReportReason.HATE_SPEECH,
                        "특정 집단을 비하합니다."
                )
        );

        ArgumentCaptor<SpeechReport> reportCaptor = ArgumentCaptor.forClass(SpeechReport.class);
        verify(speechReportRepository).save(reportCaptor.capture());
        SpeechReport savedReport = reportCaptor.getValue();
        assertThat(savedReport.getSpeechId()).isEqualTo(10L);
        assertThat(savedReport.getReportedUserId()).isEqualTo(30L);
        assertThat(savedReport.getReporterUserId()).isEqualTo(20L);
        assertThat(savedReport.getContentSnapshot()).isEqualTo("신고 대상 의견");
        assertThat(savedReport.getStatus()).isEqualTo(SpeechReportStatus.PENDING);
        assertThat(response.reportId()).isEqualTo(100L);
        assertThat(response.status()).isEqualTo(SpeechReportStatus.PENDING);
    }

    @Test
    void createReport_throwsSpeechNotFound_whenSpeechDoesNotExistOrIsDeleted() {
        given(speechRepository.findByIdAndDeletedFalse(10L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> speechReportService.createReport(
                10L,
                20L,
                new SpeechReportCreateCommand(SpeechReportReason.SPAM, null)
        ))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.SPEECH_NOT_FOUND);
    }

    @Test
    void createReport_throwsSelfReportNotAllowed_whenReporterOwnsSpeech() {
        Speech speech = Speech.createMainOpinion(1L, 20L, "내 의견", SpeechStance.CON);
        given(speechRepository.findByIdAndDeletedFalse(10L)).willReturn(Optional.of(speech));

        assertThatThrownBy(() -> speechReportService.createReport(
                10L,
                20L,
                new SpeechReportCreateCommand(SpeechReportReason.OTHER, "잘못 신고")
        ))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.SPEECH_REPORT_SELF_NOT_ALLOWED);

        verify(speechReportRepository, never())
                .save(org.mockito.ArgumentMatchers.any(SpeechReport.class));
    }

    @Test
    void createReport_throwsAlreadyExists_whenReporterAlreadyReportedSpeech() {
        Speech speech = Speech.createMainOpinion(1L, 30L, "신고 대상 의견", SpeechStance.PRO);
        given(speechRepository.findByIdAndDeletedFalse(10L)).willReturn(Optional.of(speech));
        given(speechReportRepository.existsBySpeechIdAndReporterUserId(10L, 20L))
                .willReturn(true);

        assertThatThrownBy(() -> speechReportService.createReport(
                10L,
                20L,
                new SpeechReportCreateCommand(SpeechReportReason.SPAM, null)
        ))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.SPEECH_REPORT_ALREADY_EXISTS);

        verify(speechReportRepository, never())
                .save(org.mockito.ArgumentMatchers.any(SpeechReport.class));
    }

    @Test
    void createReport_throwsDescriptionRequired_whenReasonIsOtherAndDescriptionIsBlank() {
        assertThatThrownBy(() -> speechReportService.createReport(
                10L,
                20L,
                new SpeechReportCreateCommand(SpeechReportReason.OTHER, " ")
        ))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.SPEECH_REPORT_DESCRIPTION_REQUIRED);

        verify(speechRepository, never()).findByIdAndDeletedFalse(10L);
        verify(speechReportRepository, never())
                .save(org.mockito.ArgumentMatchers.any(SpeechReport.class));
    }
}
