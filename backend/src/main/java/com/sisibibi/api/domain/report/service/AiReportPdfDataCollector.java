package com.sisibibi.api.domain.report.service;

import com.sisibibi.api.domain.report.entity.AiReport;
import com.sisibibi.api.domain.report.entity.AiReportCustomReport;
import com.sisibibi.api.domain.report.entity.AiReportPdfExport;
import com.sisibibi.api.domain.report.repository.AiReportPdfQueryRepository;
import com.sisibibi.api.domain.report.repository.AiReportRepository;
import com.sisibibi.api.domain.room.entity.Room;
import com.sisibibi.api.domain.room.repository.RoomRepository;
import com.sisibibi.api.domain.roomparticipant.repository.RoomParticipantRepository;
import com.sisibibi.api.domain.speech.entity.SpeechStance;
import com.sisibibi.api.domain.speech.repository.SpeechRepository;
import com.sisibibi.api.domain.speechreaction.repository.SpeechReactionRepository;
import com.sisibibi.api.domain.topic.entity.Topic;
import com.sisibibi.api.domain.topic.repository.TopicRepository;
import com.sisibibi.api.domain.user.entity.User;
import com.sisibibi.api.domain.user.repository.UserRepository;
import com.sisibibi.api.global.exception.CustomException;
import com.sisibibi.api.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AiReportPdfDataCollector {

    private final AiReportRepository aiReportRepository;
    private final RoomRepository roomRepository;
    private final TopicRepository topicRepository;
    private final UserRepository userRepository;
    private final RoomParticipantRepository roomParticipantRepository;
    private final SpeechRepository speechRepository;
    private final SpeechReactionRepository speechReactionRepository;
    private final AiReportPdfQueryRepository queryRepository;

    public AiReportPdfModel collect(AiReportPdfExport export) {
        AiReport report = aiReportRepository.findById(export.getAiReportId())
                .orElseThrow(() -> new CustomException(ErrorCode.AI_REPORT_NOT_FOUND));
        Room room = roomRepository.findById(report.getRoomId())
                .orElseThrow(() -> new CustomException(ErrorCode.ROOM_NOT_FOUND));
        Topic topic = topicRepository.findById(room.getTopicId())
                .orElseThrow(() -> new CustomException(ErrorCode.TOPIC_NOT_FOUND));
        User requester = userRepository.findById(export.getRequestedByUserId())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        int participantCount = roomParticipantRepository.countByRoomId(room.getId());
        long opinionCount = speechRepository.countAiReportSourceSpeeches(room.getId());
        long reactionCount = speechReactionRepository.countReactionsForCompletedSpeeches(room.getId());
        long proCount = speechRepository.countAiReportSourceSpeechesByStance(room.getId(), SpeechStance.PRO);
        long conCount = speechRepository.countAiReportSourceSpeechesByStance(room.getId(), SpeechStance.CON);

        List<AiReportPdfModel.TopOpinion> proTopOpinions =
                queryRepository.findTopOpinions(room.getId(), SpeechStance.PRO, 3);
        List<AiReportPdfModel.TopOpinion> conTopOpinions =
                queryRepository.findTopOpinions(room.getId(), SpeechStance.CON, 3);
        List<AiReportCustomReport> customReports = report.getCustomReports() == null
                ? List.of()
                : report.getCustomReports().stream()
                        .filter(r -> r.isVisibleTo(export.getRequestedByUserId()))
                        .toList();

        return new AiReportPdfModel(
                export.getId(),
                report.getId(),
                room.getId(),
                room.getTitle(),
                topic.getTitle(),
                topic.getDescription(),
                requester.getEmail(),
                requester.getNickname(),
                participantCount,
                opinionCount,
                reactionCount,
                proCount,
                conCount,
                report.getCoreLine(),
                report.getKeyIssues() == null ? List.of() : report.getKeyIssues(),
                report.getCommonGround(),
                report.getAiSummary(),
                report.getAiOpinion(),
                proTopOpinions,
                conTopOpinions,
                customReports,
                LocalDateTime.now()
        );
    }
}
