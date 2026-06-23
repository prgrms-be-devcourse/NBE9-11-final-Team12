package com.sisibibi.api.domain.report.service;

import com.sisibibi.api.domain.report.client.dto.AiReportGenerateReq;
import com.sisibibi.api.domain.report.client.dto.AiReportGenerateRes;
import com.sisibibi.api.domain.report.client.dto.AiReportRoomPayload;
import com.sisibibi.api.domain.report.client.dto.AiReportSpeechPayload;
import com.sisibibi.api.domain.report.client.dto.AiReportTopicPayload;
import com.sisibibi.api.domain.report.dto.response.AiReportRes;
import com.sisibibi.api.domain.report.entity.AiReport;
import com.sisibibi.api.domain.report.entity.AiReportCustomPrompt;
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

import java.util.List;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AiReportPersistenceService {

    private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s+");

    private final RoomRepository roomRepository;
    private final TopicRepository topicRepository;
    private final SpeechRepository speechRepository;
    private final AiReportRepository aiReportRepository;

    @Transactional
    public AiReportGenerationContext prepareGeneration(Long roomId, List<CustomPromptCommand> customPrompts) {
        Room room = roomRepository.findByIdForUpdate(roomId)
                .orElseThrow(() -> new CustomException(ErrorCode.ROOM_NOT_FOUND));

        if (room.getStatus() != RoomStatus.CLOSED) {
            throw new CustomException(ErrorCode.AI_REPORT_ROOM_NOT_CLOSED);
        }

        AiReport report = aiReportRepository.findByRoomIdForUpdate(roomId)
                .orElse(null);

        if (report != null && report.shouldSkipGeneration()) {
            return AiReportGenerationContext.skipAi(AiReportRes.from(report));
        }

        if (report == null) {
            report = aiReportRepository.save(AiReport.pending(roomId, toSnapshots(customPrompts)));
        } else {
            report.retry(toSnapshots(customPrompts));
        }

        Topic topic = topicRepository.findById(room.getTopicId())
                .orElseThrow(() -> new CustomException(ErrorCode.TOPIC_NOT_FOUND));
        AiReportGenerateReq request = buildRequest(
                room,
                topic,
                speechRepository.findAiReportSourceSpeeches(roomId),
                customPrompts
        );

        return AiReportGenerationContext.callAi(report.getId(), request);
    }

    @Transactional
    public AiReportRes complete(Long reportId, AiReportGenerateRes response) {
        AiReport report = aiReportRepository.findById(reportId)
                .orElseThrow(() -> new CustomException(ErrorCode.AI_REPORT_NOT_FOUND));

        report.complete(response);
        return AiReportRes.from(report);
    }

    @Transactional
    public AiReportRes fail(Long reportId, String errorMessage) {
        AiReport report = aiReportRepository.findById(reportId)
                .orElseThrow(() -> new CustomException(ErrorCode.AI_REPORT_NOT_FOUND));

        report.fail(errorMessage);
        return AiReportRes.from(report);
    }

    public AiReportRes getReport(Long roomId) {
        return aiReportRepository.findByRoomId(roomId)
                .map(AiReportRes::from)
                .orElseThrow(() -> new CustomException(ErrorCode.AI_REPORT_NOT_FOUND));
    }

    private AiReportGenerateReq buildRequest(
            Room room,
            Topic topic,
            List<Speech> speeches,
            List<CustomPromptCommand> customPrompts
    ) {
        return new AiReportGenerateReq(
                new AiReportRoomPayload(room.getTitle(), room.getStartedAt(), room.getEndedAt()),
                new AiReportTopicPayload(topic.getTitle(), topic.getDescription()),
                speeches.stream()
                        .map(this::toPayload)
                        .toList(),
                customPrompts
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

    private List<AiReportCustomPrompt> toSnapshots(List<CustomPromptCommand> customPrompts) {
        if (customPrompts == null || customPrompts.isEmpty()) {
            return List.of();
        }

        return customPrompts.stream()
                .map(prompt -> new AiReportCustomPrompt(prompt.label(), prompt.prompt()))
                .toList();
    }
}
