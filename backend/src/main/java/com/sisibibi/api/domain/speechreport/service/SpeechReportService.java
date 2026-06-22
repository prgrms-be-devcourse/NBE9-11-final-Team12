package com.sisibibi.api.domain.speechreport.service;

import com.sisibibi.api.domain.speech.entity.Speech;
import com.sisibibi.api.domain.speech.repository.SpeechRepository;
import com.sisibibi.api.domain.speechreport.dto.command.SpeechReportCreateCommand;
import com.sisibibi.api.domain.speechreport.dto.response.SpeechReportCreateRes;
import com.sisibibi.api.domain.speechreport.dto.response.SpeechReportDetailRes;
import com.sisibibi.api.domain.speechreport.dto.response.SpeechReportSummaryRes;
import com.sisibibi.api.domain.speechreport.dto.response.SpeechReportReviewRes;
import com.sisibibi.api.domain.speechreport.entity.SpeechReport;
import com.sisibibi.api.domain.speechreport.entity.SpeechReportReason;
import com.sisibibi.api.domain.speechreport.entity.SpeechReportReviewAction;
import com.sisibibi.api.domain.speechreport.entity.SpeechReportStatus;
import com.sisibibi.api.domain.speechreport.entity.ViolationSeverity;
import com.sisibibi.api.domain.speechreport.repository.SpeechReportRepository;
import com.sisibibi.api.global.exception.CustomException;
import com.sisibibi.api.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class SpeechReportService {

    private final SpeechRepository speechRepository;
    private final SpeechReportRepository speechReportRepository;

    @Transactional(readOnly = true)
    public Page<SpeechReportSummaryRes> getReports(
            SpeechReportStatus status,
            SpeechReportReason reason,
            Pageable pageable
    ) {
        return speechReportRepository.findAllByFilters(status, reason, pageable)
                .map(SpeechReportSummaryRes::from);
    }

    @Transactional(readOnly = true)
    public SpeechReportDetailRes getReport(Long reportId) {
        SpeechReport report = speechReportRepository.findById(reportId)
                .orElseThrow(() -> new CustomException(ErrorCode.SPEECH_REPORT_NOT_FOUND));

        return SpeechReportDetailRes.from(report);
    }

    @Transactional
    public SpeechReportReviewRes reviewReport(
            Long reportId,
            Long reviewerUserId,
            SpeechReportReviewAction action,
            String resolutionNote,
            ViolationSeverity severity
    ) {
        SpeechReport report = speechReportRepository.findByIdForUpdate(reportId)
                .orElseThrow(() -> new CustomException(ErrorCode.SPEECH_REPORT_NOT_FOUND));

        report.review(action, reviewerUserId, resolutionNote, severity, LocalDateTime.now());
        log.info(
                "Speech report reviewed. reportId={}, reviewerUserId={}, action={}, status={}, severity={}",
                reportId,
                reviewerUserId,
                action,
                report.getStatus(),
                report.getSeverity()
        );

        return SpeechReportReviewRes.from(report);
    }

    @Transactional
    public SpeechReportCreateRes createReport(
            Long speechId,
            Long reporterUserId,
            SpeechReportCreateCommand command
    ) {
        if (command.reason() == SpeechReportReason.OTHER
                && (command.description() == null || command.description().isBlank())) {
            throw new CustomException(ErrorCode.SPEECH_REPORT_DESCRIPTION_REQUIRED);
        }

        Speech speech = speechRepository.findByIdAndDeletedFalse(speechId)
                .orElseThrow(() -> new CustomException(ErrorCode.SPEECH_NOT_FOUND));

        if (speech.getUserId().equals(reporterUserId)) {
            log.warn(
                    "Self speech report blocked. speechId={}, reporterUserId={}",
                    speechId,
                    reporterUserId
            );
            throw new CustomException(ErrorCode.SPEECH_REPORT_SELF_NOT_ALLOWED);
        }

        if (speechReportRepository.existsBySpeechIdAndReporterUserId(
                speechId,
                reporterUserId
        )) {
            log.warn(
                    "Duplicate speech report blocked. speechId={}, reporterUserId={}",
                    speechId,
                    reporterUserId
            );
            throw new CustomException(ErrorCode.SPEECH_REPORT_ALREADY_EXISTS);
        }

        SpeechReport report = SpeechReport.create(
                speechId,
                speech.getUserId(),
                reporterUserId,
                speech.getContent(),
                command.reason(),
                command.description()
        );

        SpeechReport savedReport = speechReportRepository.save(report);
        log.info(
                "Speech report created. reportId={}, speechId={}, reporterUserId={}, reportedUserId={}, reason={}",
                savedReport.getId(),
                speechId,
                reporterUserId,
                speech.getUserId(),
                command.reason()
        );

        return SpeechReportCreateRes.from(savedReport);
    }
}
