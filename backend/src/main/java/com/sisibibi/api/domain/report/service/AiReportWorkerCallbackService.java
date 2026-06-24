package com.sisibibi.api.domain.report.service;

import com.sisibibi.api.domain.report.client.AiReportProperties;
import com.sisibibi.api.domain.report.client.dto.AiReportBaseReportPayload;
import com.sisibibi.api.domain.report.client.dto.AiReportGenerateReq;
import com.sisibibi.api.domain.report.client.dto.AiReportGenerateRes;
import com.sisibibi.api.domain.report.client.dto.AiReportRoomPayload;
import com.sisibibi.api.domain.report.client.dto.AiReportSpeechPayload;
import com.sisibibi.api.domain.report.client.dto.AiReportTopicPayload;
import com.sisibibi.api.domain.report.dto.request.AiReportWorkerCompleteReq;
import com.sisibibi.api.domain.report.dto.request.AiReportWorkerFailReq;
import com.sisibibi.api.domain.report.dto.response.AiReportRes;
import com.sisibibi.api.domain.report.dto.response.AiReportWorkerProcessingRes;
import com.sisibibi.api.domain.report.entity.AiReport;
import com.sisibibi.api.domain.report.entity.AiReportCustomPrompt;
import com.sisibibi.api.domain.report.entity.AiReportStatus;
import com.sisibibi.api.domain.report.prompt.CustomPromptCommand;
import com.sisibibi.api.domain.report.repository.AiReportRepository;
import com.sisibibi.api.domain.room.entity.Room;
import com.sisibibi.api.domain.room.entity.RoomStatus;
import com.sisibibi.api.domain.room.repository.RoomRepository;
import com.sisibibi.api.domain.speech.entity.Speech;
import com.sisibibi.api.domain.speech.repository.SpeechRepository;
import com.sisibibi.api.domain.topic.entity.Topic;
import com.sisibibi.api.domain.topic.repository.TopicRepository;
import com.sisibibi.api.global.exception.CustomException;
import com.sisibibi.api.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AiReportWorkerCallbackService {

    private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s+");

    private final RoomRepository roomRepository;
    private final TopicRepository topicRepository;
    private final SpeechRepository speechRepository;
    private final AiReportRepository aiReportRepository;
    private final AiReportProperties aiReportProperties;

    @Transactional
    public AiReportWorkerProcessingRes startProcessing(Long reportId, AiReportGenerationType generationType) {
        AiReport report = findReportForUpdate(reportId);
        Room room = roomRepository.findByIdForUpdate(report.getRoomId())
                .orElseThrow(() -> new CustomException(ErrorCode.ROOM_NOT_FOUND));

        if (room.getStatus() != RoomStatus.CLOSED) {
            throw new CustomException(ErrorCode.AI_REPORT_ROOM_NOT_CLOSED);
        }

        Topic topic = topicRepository.findById(room.getTopicId())
                .orElseThrow(() -> new CustomException(ErrorCode.TOPIC_NOT_FOUND));

        report.markProcessing(LocalDateTime.now(), aiReportProperties.getTimeout());

        AiReportGenerateReq request = buildRequest(
                room,
                topic,
                speechRepository.findAiReportSourceSpeeches(room.getId()),
                generationType == AiReportGenerationType.CUSTOM_ONLY ? toBasePayload(report) : null,
                generationType == AiReportGenerationType.BASE_ONLY ? List.of() : toCommands(report.getCustomPrompts())
        );

        return new AiReportWorkerProcessingRes(
                report.getId(),
                report.getRoomId(),
                generationType,
                request
        );
    }

    @Transactional
    public AiReportRes complete(Long reportId, AiReportWorkerCompleteReq request) {
        AiReport report = findReportForUpdate(reportId);
        if (report.getStatus() == AiReportStatus.COMPLETED) {
            return AiReportRes.from(report);
        }

        AiReportGenerateRes response = request.toGenerateResponse();
        if (request.generationType() == AiReportGenerationType.CUSTOM_ONLY) {
            validateCustomReports(response, report.getCustomPrompts().size());
            report.completeCustomReports(response);
            return AiReportRes.from(report);
        }

        validateBaseReport(response);
        if (request.generationType() == AiReportGenerationType.BASE_WITH_CUSTOM) {
            validateCustomReports(response, report.getCustomPrompts().size());
        }

        report.complete(response);
        return AiReportRes.from(report);
    }

    @Transactional
    public AiReportRes fail(Long reportId, AiReportWorkerFailReq request) {
        AiReport report = findReportForUpdate(reportId);
        if (report.getStatus() == AiReportStatus.COMPLETED) {
            return AiReportRes.from(report);
        }

        report.fail(request.errorCode(), request.errorMessage());
        return AiReportRes.from(report);
    }

    private AiReport findReportForUpdate(Long reportId) {
        return aiReportRepository.findByIdForUpdate(reportId)
                .orElseThrow(() -> new CustomException(ErrorCode.AI_REPORT_NOT_FOUND));
    }

    private void validateBaseReport(AiReportGenerateRes response) {
        if (!response.hasBaseRequiredFields()) {
            throw new CustomException(ErrorCode.AI_REPORT_INVALID_RESPONSE);
        }
    }

    private void validateCustomReports(AiReportGenerateRes response, int expectedCount) {
        if (!response.hasCustomReports(expectedCount)) {
            throw new CustomException(ErrorCode.AI_REPORT_INVALID_RESPONSE);
        }
    }

    private AiReportGenerateReq buildRequest(
            Room room,
            Topic topic,
            List<Speech> speeches,
            AiReportBaseReportPayload baseReport,
            List<CustomPromptCommand> customPrompts
    ) {
        return new AiReportGenerateReq(
                new AiReportRoomPayload(room.getTitle(), room.getStartedAt(), room.getEndedAt()),
                new AiReportTopicPayload(topic.getTitle(), topic.getDescription()),
                speeches.stream()
                        .map(this::toPayload)
                        .toList(),
                baseReport,
                customPrompts
        );
    }

    private AiReportBaseReportPayload toBasePayload(AiReport report) {
        return new AiReportBaseReportPayload(
                report.getCoreLine(),
                report.getKeyIssues(),
                report.getAiSummary(),
                report.getCommonGround(),
                report.getAiOpinion()
        );
    }

    private AiReportSpeechPayload toPayload(Speech speech) {
        return new AiReportSpeechPayload(
                speech.getId(),
                speech.getUserId(),
                speech.getStance(),
                compactContent(speech.getContent()),
                speech.getStartedAt(),
                speech.getEndedAt(),
                speech.getCreatedAt()
        );
    }

    private String compactContent(String content) {
        if (content == null) {
            return "";
        }

        return WHITESPACE_PATTERN.matcher(content.trim()).replaceAll(" ");
    }

    private List<CustomPromptCommand> toCommands(List<AiReportCustomPrompt> customPrompts) {
        if (customPrompts == null || customPrompts.isEmpty()) {
            return List.of();
        }

        return customPrompts.stream()
                .map(prompt -> new CustomPromptCommand(prompt.label(), prompt.prompt()))
                .toList();
    }
}
