package com.sisibibi.api.domain.speechreport.service;

import com.sisibibi.api.domain.speech.entity.Speech;
import com.sisibibi.api.domain.speech.entity.SpeechDeleteReason;
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
import com.sisibibi.api.domain.speechreport.entity.OffTopicAiReview;
import com.sisibibi.api.domain.speechreport.repository.OffTopicAiReviewRepository;
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
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SpeechReportService {

    private final SpeechRepository speechRepository;
    private final SpeechReportRepository speechReportRepository;
    private final OffTopicAiReviewRepository offTopicAiReviewRepository;
    private final OffTopicAiReviewService offTopicAiReviewService;

    @Transactional(readOnly = true)
    public Page<SpeechReportSummaryRes> getReports(
            SpeechReportStatus status,
            SpeechReportReason reason,
            Pageable pageable
    ) {
        Page<SpeechReport> reports =
                speechReportRepository.findAllByFilters(status, reason, pageable);
        Map<Long, OffTopicAiReview> reviewsBySpeechId =
                findOffTopicAiReviewsBySpeechId(reports.getContent());

        return reports.map(report -> SpeechReportSummaryRes.from(
                report,
                reviewsBySpeechId.get(report.getSpeechId())
        ));
    }

    @Transactional(readOnly = true)
    public SpeechReportDetailRes getReport(Long reportId) {
        SpeechReport report = speechReportRepository.findById(reportId)
                .orElseThrow(() -> new CustomException(ErrorCode.SPEECH_REPORT_NOT_FOUND));

        OffTopicAiReview review = offTopicAiReviewRepository.findBySpeechId(report.getSpeechId())
                .orElse(null);

        return SpeechReportDetailRes.from(report, review);
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
        softDeleteSpeechIfOffTopicResolved(report, action);
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
        if (command.reason() == SpeechReportReason.OFF_TOPIC) {
            offTopicAiReviewService.triggerIfNeeded(speech, command.reason());
        }
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

    private Map<Long, OffTopicAiReview> findOffTopicAiReviewsBySpeechId(
            java.util.List<SpeechReport> reports
    ) {
        if (reports.isEmpty()) {
            return Map.of();
        }

        java.util.List<Long> speechIds = reports.stream()
                .map(SpeechReport::getSpeechId)
                .distinct()
                .toList();

        java.util.List<OffTopicAiReview> reviews =
                offTopicAiReviewRepository.findBySpeechIdIn(speechIds);
        if (reviews == null || reviews.isEmpty()) {
            return Map.of();
        }

        return reviews.stream()
                .collect(Collectors.toMap(
                        OffTopicAiReview::getSpeechId,
                        Function.identity()
                ));
    }

    private void softDeleteSpeechIfOffTopicResolved(
            SpeechReport report,
            SpeechReportReviewAction action
    ) {
        if (action != SpeechReportReviewAction.RESOLVE
                || report.getReason() != SpeechReportReason.OFF_TOPIC) {
            return;
        }

        speechRepository.findByIdAndDeletedFalse(report.getSpeechId())
                .ifPresent(speech -> speech.softDeleteByModerator(
                        SpeechDeleteReason.OFF_TOPIC,
                        LocalDateTime.now()
                ));
    }
}
