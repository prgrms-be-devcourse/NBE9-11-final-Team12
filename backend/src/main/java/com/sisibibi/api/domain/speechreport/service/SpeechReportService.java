package com.sisibibi.api.domain.speechreport.service;

import com.sisibibi.api.domain.speech.entity.Speech;
import com.sisibibi.api.domain.speech.entity.SpeechDeleteReason;
import com.sisibibi.api.domain.speech.repository.SpeechRepository;
import com.sisibibi.api.domain.speechreport.dto.command.SpeechReportCreateCommand;
import com.sisibibi.api.domain.speechreport.dto.response.SpeechReportCreateRes;
import com.sisibibi.api.domain.speechreport.dto.response.SpeechReportDetailRes;
import com.sisibibi.api.domain.speechreport.dto.response.SpeechReportReviewRes;
import com.sisibibi.api.domain.speechreport.dto.response.SpeechReportSummaryRes;
import com.sisibibi.api.domain.speechreport.entity.OffTopicAiReview;
import com.sisibibi.api.domain.speechreport.entity.SpeechReport;
import com.sisibibi.api.domain.speechreport.entity.SpeechReportReason;
import com.sisibibi.api.domain.speechreport.entity.SpeechReportReviewAction;
import com.sisibibi.api.domain.speechreport.entity.SpeechReportStatus;
import com.sisibibi.api.domain.speechreport.entity.ViolationSeverity;
import com.sisibibi.api.domain.speechreport.repository.OffTopicAiReviewRepository;
import com.sisibibi.api.domain.speechreport.repository.SpeechReportRepository;
import com.sisibibi.api.domain.user.service.UserNicknameProvider;
import com.sisibibi.api.global.exception.CustomException;
import com.sisibibi.api.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    private final UserNicknameProvider userNicknameProvider;

    @Transactional(readOnly = true)
    public Page<SpeechReportSummaryRes> getReports(
            SpeechReportStatus status,
            SpeechReportReason reason,
            Pageable pageable
    ) {
        Page<SpeechReport> reports = speechReportRepository.findAllByFilters(status, reason, pageable);
        Map<Long, String> nicknames = userNicknameProvider.findNicknamesByIds(extractUserIds(reports));
        Map<Long, OffTopicAiReview> reviewsBySpeechId =
                findOffTopicAiReviewsBySpeechId(reports.getContent());

        return reports.map(report -> SpeechReportSummaryRes.from(
                report,
                nicknames.get(report.getReportedUserId()),
                nicknames.get(report.getReporterUserId()),
                reviewsBySpeechId.get(report.getSpeechId())
        ));
    }

    @Transactional(readOnly = true)
    public SpeechReportDetailRes getReport(Long reportId) {
        SpeechReport report = speechReportRepository.findById(reportId)
                .orElseThrow(() -> new CustomException(ErrorCode.SPEECH_REPORT_NOT_FOUND));
        Map<Long, String> nicknames = userNicknameProvider.findNicknamesByIds(extractUserIds(report));
        OffTopicAiReview review = offTopicAiReviewRepository.findBySpeechId(report.getSpeechId())
                .orElse(null);

        return SpeechReportDetailRes.from(
                report,
                nicknameOf(nicknames, report.getReportedUserId()),
                nicknameOf(nicknames, report.getReporterUserId()),
                nicknameOf(nicknames, report.getReviewedBy()),
                review
        );
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

        SpeechReportStatus previousStatus = report.getStatus();
        report.review(action, reviewerUserId, resolutionNote, severity, LocalDateTime.now());
        softDeleteSpeechIfOffTopicResolved(report, previousStatus);
        log.info(
                "Speech report reviewed. reportId={}, reviewerUserId={}, action={}, status={}, severity={}",
                reportId,
                reviewerUserId,
                action,
                report.getStatus(),
                report.getSeverity()
        );

        Map<Long, String> nicknames = userNicknameProvider.findNicknamesByIds(Set.of(reviewerUserId));
        return SpeechReportReviewRes.from(report, nicknameOf(nicknames, reviewerUserId));
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
            List<SpeechReport> reports
    ) {
        if (reports.isEmpty()) {
            return Map.of();
        }

        List<Long> speechIds = reports.stream()
                .map(SpeechReport::getSpeechId)
                .distinct()
                .toList();

        List<OffTopicAiReview> reviews =
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
            SpeechReportStatus previousStatus
    ) {
        if (previousStatus == SpeechReportStatus.RESOLVED
                || report.getStatus() != SpeechReportStatus.RESOLVED
                || report.getReason() != SpeechReportReason.OFF_TOPIC) {
            return;
        }

        speechRepository.findByIdAndDeletedFalse(report.getSpeechId())
                .ifPresent(speech -> speech.softDeleteByModerator(
                        SpeechDeleteReason.OFF_TOPIC,
                        LocalDateTime.now()
                ));
    }

    private Set<Long> extractUserIds(Page<SpeechReport> reports) {
        Set<Long> userIds = new HashSet<>();
        reports.forEach(report -> {
            userIds.add(report.getReportedUserId());
            userIds.add(report.getReporterUserId());
            if (report.getReviewedBy() != null) {
                userIds.add(report.getReviewedBy());
            }
        });
        return userIds;
    }

    private Set<Long> extractUserIds(SpeechReport report) {
        Set<Long> userIds = new HashSet<>();
        userIds.add(report.getReportedUserId());
        userIds.add(report.getReporterUserId());
        if (report.getReviewedBy() != null) {
            userIds.add(report.getReviewedBy());
        }
        return userIds;
    }

    private String nicknameOf(Map<Long, String> nicknames, Long userId) {
        return userId == null ? null : nicknames.get(userId);
    }
}
