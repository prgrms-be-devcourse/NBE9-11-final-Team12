package com.sisibibi.api.domain.report.service;

import com.sisibibi.api.domain.report.dto.request.AiReportWorkerCompleteReq;
import com.sisibibi.api.domain.report.dto.request.AiReportWorkerFailReq;
import com.sisibibi.api.domain.report.dto.response.AiReportRes;
import com.sisibibi.api.domain.report.dto.response.AiReportWorkerProcessingRes;
import com.sisibibi.api.domain.report.client.AiReportProperties;
import com.sisibibi.api.domain.report.entity.AiReport;
import com.sisibibi.api.domain.report.entity.AiReportCustomPrompt;
import com.sisibibi.api.domain.report.entity.AiReportStatus;
import com.sisibibi.api.domain.report.repository.AiReportRepository;
import com.sisibibi.api.domain.room.entity.Room;
import com.sisibibi.api.domain.room.entity.RoomStatus;
import com.sisibibi.api.domain.room.repository.RoomRepository;
import com.sisibibi.api.domain.speech.entity.Speech;
import com.sisibibi.api.domain.speech.entity.SpeechStance;
import com.sisibibi.api.domain.speech.entity.SpeechStatus;
import com.sisibibi.api.domain.speech.repository.SpeechRepository;
import com.sisibibi.api.domain.topic.entity.Topic;
import com.sisibibi.api.domain.topic.repository.TopicRepository;
import com.sisibibi.api.global.exception.CustomException;
import com.sisibibi.api.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AiReportWorkerCallbackServiceTest {

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private TopicRepository topicRepository;

    @Mock
    private SpeechRepository speechRepository;

    @Mock
    private AiReportRepository aiReportRepository;

    @Mock
    private AiReportProperties aiReportProperties;

    @InjectMocks
    private AiReportWorkerCallbackService callbackService;

    @Test
    void startProcessing_marksReportProcessingAndReturnsGenerationInput() {
        AiReport report = reportWithId(AiReport.requested(
                10L,
                List.of(new AiReportCustomPrompt(7L, "custom 1", "minority view"))
        ), 55L);
        report.markQueued();
        Room room = closedRoom(10L, 1L, "room title");
        Topic topic = Topic.approved("topic title", "topic description", "IT", "https://example.com");
        ReflectionTestUtils.setField(topic, "id", 1L);
        Speech speech = completedSpeech(100L, 10L, 7L, "speech content", SpeechStance.PRO);

        given(aiReportRepository.findByIdForUpdate(55L)).willReturn(Optional.of(report));
        given(roomRepository.findByIdForUpdate(10L)).willReturn(Optional.of(room));
        given(topicRepository.findById(1L)).willReturn(Optional.of(topic));
        given(speechRepository.findAiReportSourceSpeeches(10L)).willReturn(List.of(speech));
        given(aiReportProperties.getTimeout()).willReturn(Duration.ofMinutes(5));

        AiReportWorkerProcessingRes result =
                callbackService.startProcessing(55L, AiReportGenerationType.BASE_WITH_CUSTOM);

        assertThat(report.getStatus()).isEqualTo(AiReportStatus.PROCESSING);
        assertThat(report.getProcessingStartedAt()).isNotNull();
        assertThat(report.getProcessingLockedUntil()).isAfter(report.getProcessingStartedAt());
        assertThat(result.reportId()).isEqualTo(55L);
        assertThat(result.roomId()).isEqualTo(10L);
        assertThat(result.generationType()).isEqualTo(AiReportGenerationType.BASE_WITH_CUSTOM);
        assertThat(result.request().room().title()).isEqualTo("room title");
        assertThat(result.request().topic().title()).isEqualTo("topic title");
        assertThat(result.request().speeches()).hasSize(1);
        assertThat(result.request().customPrompts()).hasSize(1);
    }

    @Test
    void complete_savesBaseReportResult() {
        AiReport report = reportWithId(AiReport.requested(10L), 55L);
        report.markQueued();
        report.markProcessing(LocalDateTime.of(2026, 6, 25, 12, 0), Duration.ofMinutes(5));
        given(aiReportRepository.findByIdForUpdate(55L)).willReturn(Optional.of(report));

        AiReportRes result = callbackService.complete(55L, new AiReportWorkerCompleteReq(
                AiReportGenerationType.BASE_ONLY,
                "core",
                List.of("issue"),
                "summary",
                "common",
                "opinion",
                List.of()
        ));

        assertThat(report.getStatus()).isEqualTo(AiReportStatus.COMPLETED);
        assertThat(report.getCoreLine()).isEqualTo("core");
        assertThat(report.getProcessingStartedAt()).isNull();
        assertThat(result.status()).isEqualTo("COMPLETED");
    }

    @Test
    void complete_appendsCustomOnlyResultWithoutClearingBaseReport() {
        AiReport report = reportWithId(AiReport.requested(10L), 55L);
        report.complete(new com.sisibibi.api.domain.report.client.dto.AiReportGenerateRes(
                "base core",
                List.of("base issue"),
                "base summary",
                "base common",
                "base opinion"
        ));
        report.requestCustomReports(List.of(new AiReportCustomPrompt(7L, "custom 1", "minority view")));
        report.markQueued();
        report.markProcessing(LocalDateTime.of(2026, 6, 25, 12, 0), Duration.ofMinutes(5));
        given(aiReportRepository.findByIdForUpdate(55L)).willReturn(Optional.of(report));

        AiReportRes result = callbackService.complete(55L, new AiReportWorkerCompleteReq(
                AiReportGenerationType.CUSTOM_ONLY,
                null,
                List.of(),
                null,
                null,
                null,
                List.of(new com.sisibibi.api.domain.report.client.dto.AiReportCustomReportPayload(
                        "custom result",
                        "custom content"
                ))
        ));

        assertThat(report.getStatus()).isEqualTo(AiReportStatus.COMPLETED);
        assertThat(report.getCoreLine()).isEqualTo("base core");
        assertThat(report.getCustomReports()).hasSize(1);
        assertThat(report.getCustomReports().get(0).userId()).isEqualTo(7L);
        assertThat(result.customReports()).hasSize(1);
    }

    @Test
    void complete_returnsCurrentReportWithoutMutation_whenReportIsAlreadyCompleted() {
        AiReport report = reportWithId(AiReport.requested(10L), 55L);
        report.complete(new com.sisibibi.api.domain.report.client.dto.AiReportGenerateRes(
                "base core",
                List.of("base issue"),
                "base summary",
                "base common",
                "base opinion"
        ));
        given(aiReportRepository.findByIdForUpdate(55L)).willReturn(Optional.of(report));

        AiReportRes result = callbackService.complete(55L, new AiReportWorkerCompleteReq(
                AiReportGenerationType.BASE_ONLY,
                "duplicate core",
                List.of("duplicate issue"),
                "duplicate summary",
                "duplicate common",
                "duplicate opinion",
                List.of()
        ));

        assertThat(report.getCoreLine()).isEqualTo("base core");
        assertThat(result.coreLine()).isEqualTo("base core");
    }

    @Test
    void complete_rejectsInvalidBaseResult() {
        AiReport report = reportWithId(AiReport.requested(10L), 55L);
        report.markProcessing(LocalDateTime.of(2026, 6, 25, 12, 0), Duration.ofMinutes(5));
        given(aiReportRepository.findByIdForUpdate(55L)).willReturn(Optional.of(report));

        assertThatThrownBy(() -> callbackService.complete(55L, new AiReportWorkerCompleteReq(
                AiReportGenerationType.BASE_ONLY,
                "",
                List.of(),
                "summary",
                "common",
                "opinion",
                List.of()
        )))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.AI_REPORT_INVALID_RESPONSE);
    }

    @Test
    void fail_savesGenerationFailedUnlessAlreadyCompleted() {
        AiReport report = reportWithId(AiReport.requested(10L), 55L);
        report.markProcessing(LocalDateTime.of(2026, 6, 25, 12, 0), Duration.ofMinutes(5));
        given(aiReportRepository.findByIdForUpdate(55L)).willReturn(Optional.of(report));

        AiReportRes result = callbackService.fail(
                55L,
                new AiReportWorkerFailReq("LLM_TIMEOUT", "local LLM timed out")
        );

        assertThat(report.getStatus()).isEqualTo(AiReportStatus.GENERATION_FAILED);
        assertThat(report.getLastErrorCode()).isEqualTo("LLM_TIMEOUT");
        assertThat(report.getLastErrorMessage()).isEqualTo("local LLM timed out");
        assertThat(result.status()).isEqualTo("GENERATION_FAILED");
    }

    @Test
    void startProcessing_throwsNotFound_whenReportDoesNotExist() {
        given(aiReportRepository.findByIdForUpdate(55L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> callbackService.startProcessing(55L, AiReportGenerationType.BASE_ONLY))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.AI_REPORT_NOT_FOUND);

        verify(roomRepository, never()).findByIdForUpdate(10L);
    }

    private AiReport reportWithId(AiReport report, Long id) {
        ReflectionTestUtils.setField(report, "id", id);
        return report;
    }

    private Room closedRoom(Long roomId, Long topicId, String title) {
        Room room = Room.open(
                topicId,
                title,
                LocalDateTime.of(2026, 6, 25, 10, 0),
                LocalDateTime.of(2026, 6, 25, 12, 0),
                10
        );
        ReflectionTestUtils.setField(room, "id", roomId);
        room.close(LocalDateTime.of(2026, 6, 25, 12, 0));
        assertThat(room.getStatus()).isEqualTo(RoomStatus.CLOSED);
        return room;
    }

    private Speech completedSpeech(Long speechId, Long roomId, Long userId, String content, SpeechStance stance) {
        Speech speech = Speech.createMainOpinion(roomId, userId, content, stance);
        ReflectionTestUtils.setField(speech, "id", speechId);
        ReflectionTestUtils.setField(speech, "status", SpeechStatus.COMPLETED);
        ReflectionTestUtils.setField(speech, "createdAt", LocalDateTime.of(2026, 6, 25, 10, 30));
        ReflectionTestUtils.setField(speech, "startedAt", LocalDateTime.of(2026, 6, 25, 10, 0));
        ReflectionTestUtils.setField(speech, "endedAt", LocalDateTime.of(2026, 6, 25, 10, 3));
        return speech;
    }
}
