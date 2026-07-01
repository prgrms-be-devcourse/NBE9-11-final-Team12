package com.sisibibi.api.domain.report.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sisibibi.api.domain.report.client.dto.AiReportGenerateReq;
import com.sisibibi.api.domain.report.client.dto.AiReportGenerateRes;
import com.sisibibi.api.domain.report.client.dto.AiReportCustomReportPayload;
import com.sisibibi.api.domain.report.entity.AiReportCustomReport;
import com.sisibibi.api.domain.report.entity.AiReportCustomPrompt;
import com.sisibibi.api.domain.report.entity.AiReport;
import com.sisibibi.api.domain.report.entity.AiReportPdfExport;
import com.sisibibi.api.domain.report.entity.AiReportPdfType;
import com.sisibibi.api.domain.report.entity.AiReportStatus;
import com.sisibibi.api.domain.report.prompt.CustomPromptCommand;
import com.sisibibi.api.domain.report.repository.AiReportPdfExportRepository;
import com.sisibibi.api.domain.report.repository.AiReportRepository;
import com.sisibibi.api.domain.report.dto.response.AiReportRes;
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
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AiReportPersistenceServiceTest {

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private TopicRepository topicRepository;

    @Mock
    private SpeechRepository speechRepository;

    @Mock
    private AiReportRepository aiReportRepository;

    @Mock
    private AiReportPdfExportRepository aiReportPdfExportRepository;

    @InjectMocks
    private AiReportPersistenceService aiReportPersistenceService;

    @Test
    void prepareGeneration_buildsBaseOnlyRequest_whenReportDoesNotExistAndNoCustomPrompts() throws Exception {
        Room room = closedRoom(10L, 1L, "room title");
        Topic topic = Topic.approved("topic title", "topic description", "IT", "https://example.com");
        ReflectionTestUtils.setField(topic, "id", 1L);
        Speech speech = completedSpeech(
                100L,
                10L,
                7L,
                "base speech",
                SpeechStance.PRO,
                LocalDateTime.of(2026, 6, 22, 10, 0)
        );

        given(roomRepository.findByIdForUpdate(10L)).willReturn(Optional.of(room));
        given(aiReportRepository.findByRoomIdForUpdate(10L)).willReturn(Optional.empty());
        given(topicRepository.findById(1L)).willReturn(Optional.of(topic));
        given(speechRepository.findAiReportSourceSpeeches(10L)).willReturn(List.of(speech));
        given(aiReportRepository.save(any(AiReport.class))).willAnswer(invocation -> {
            AiReport report = invocation.getArgument(0);
            ReflectionTestUtils.setField(report, "id", 55L);
            return report;
        });

        AiReportGenerationContext context = aiReportPersistenceService.prepareGeneration(10L, List.of());

        String requestJson = new ObjectMapper().findAndRegisterModules().writeValueAsString(context.request());

        assertThat(context.shouldCallAi()).isTrue();
        assertThat(context.generationType()).isEqualTo(AiReportGenerationType.BASE_ONLY);
        assertThat(context.reportId()).isEqualTo(55L);
        assertThat(context.request().customPrompts()).isEmpty();
        assertThat(context.request().baseReport()).isNull();
        assertThat(requestJson).contains("\"speeches\":[");
        assertThat(requestJson).contains("\"speechId\":100");
        assertThat(requestJson).contains("\"userId\":7");
        assertThat(requestJson).contains("\"stance\":\"PRO\"");
        assertThat(requestJson).doesNotContain("nickname");
        assertThat(requestJson).doesNotContain("email");
        assertThat(requestJson).doesNotContain("password");
    }

    @Test
    void prepareGeneration_savesPendingReportAndBuildsMinimalAiRequest() throws Exception {
        Room room = closedRoom(10L, 1L, "토론방 제목");
        Topic topic = Topic.approved("토픽 제목", "토픽 설명", "IT", "https://example.com");
        ReflectionTestUtils.setField(topic, "id", 1L);
        Speech speech = completedSpeech(
                100L,
                10L,
                7L,
                "  발언   내용\n정리  ",
                SpeechStance.PRO,
                LocalDateTime.of(2026, 6, 22, 10, 0)
        );

        given(roomRepository.findByIdForUpdate(10L)).willReturn(Optional.of(room));
        given(aiReportRepository.findByRoomIdForUpdate(10L)).willReturn(Optional.empty());
        given(topicRepository.findById(1L)).willReturn(Optional.of(topic));
        given(speechRepository.findAiReportSourceSpeeches(10L)).willReturn(List.of(speech));
        given(aiReportRepository.save(any(AiReport.class))).willAnswer(invocation -> {
            AiReport report = invocation.getArgument(0);
            ReflectionTestUtils.setField(report, "id", 55L);
            return report;
        });

        List<CustomPromptCommand> customPrompts = List.of(
                new CustomPromptCommand("custom 1", "핵심 쟁점을 더 자세히 정리해줘")
        );

        AiReportGenerationContext context = aiReportPersistenceService.prepareGeneration(10L, customPrompts);

        ArgumentCaptor<AiReport> reportCaptor = ArgumentCaptor.forClass(AiReport.class);
        verify(aiReportRepository).save(reportCaptor.capture());
        verify(speechRepository).findAiReportSourceSpeeches(10L);

        AiReportGenerateReq request = context.request();
        String requestJson = new ObjectMapper().findAndRegisterModules().writeValueAsString(request);

        assertThat(context.shouldCallAi()).isTrue();
        assertThat(context.generationType()).isEqualTo(AiReportGenerationType.BASE_WITH_CUSTOM);
        assertThat(context.reportId()).isEqualTo(55L);
        assertThat(reportCaptor.getValue().getRoomId()).isEqualTo(10L);
        assertThat(reportCaptor.getValue().getCustomPrompts())
                .containsExactly(new AiReportCustomPrompt("custom 1", "핵심 쟁점을 더 자세히 정리해줘"));
        assertThat(requestJson).contains("\"title\":\"토론방 제목\"");
        assertThat(requestJson).contains("\"title\":\"토픽 제목\"");
        assertThat(requestJson).contains("\"speechId\":100");
        assertThat(requestJson).contains("\"userId\":7");
        assertThat(requestJson).contains("\"stance\":\"PRO\"");
        assertThat(requestJson).contains("\"content\":\"발언 내용 정리\"");
        assertThat(requestJson).contains("\"customPrompts\":[{\"label\":\"custom 1\",\"prompt\":\"핵심 쟁점을 더 자세히 정리해줘\"}]");
        assertThat(requestJson).contains("\"baseReport\":null");
        assertThat(requestJson).doesNotContain("nickname");
        assertThat(requestJson).doesNotContain("email");
        assertThat(requestJson).doesNotContain("password");
        assertThat(requestJson).doesNotContain("linkUrl");
        assertThat(requestJson).doesNotContain("imageUrl");
        assertThat(requestJson).doesNotContain("updatedAt");
    }

    @Test
    void prepareGeneration_buildsCustomOnlyRequestWithSavedBaseReport_whenBaseReportIsCompleted() throws Exception {
        Room room = closedRoom(10L, 1L, "토론방 제목");
        Topic topic = Topic.approved("토픽 제목", "토픽 설명", "IT", "https://example.com");
        ReflectionTestUtils.setField(topic, "id", 1L);
        Speech speech = completedSpeech(
                100L,
                10L,
                7L,
                "표현의 자유 반박 발언",
                SpeechStance.CON,
                LocalDateTime.of(2026, 6, 22, 10, 0)
        );
        AiReport existing = AiReport.pending(10L);
        ReflectionTestUtils.setField(existing, "id", 55L);
        existing.complete(new AiReportGenerateRes(
                "기본 핵심 한줄",
                List.of("기본 쟁점"),
                "기본 종합 정리",
                "기본 공통 의견",
                "기본 개인 소견",
                List.of()
        ));
        List<CustomPromptCommand> customPrompts = List.of(
                new CustomPromptCommand("custom 1", "표현의 자유를 반박하는 의견 정리해줘")
        );

        given(roomRepository.findByIdForUpdate(10L)).willReturn(Optional.of(room));
        given(aiReportRepository.findByRoomIdForUpdate(10L)).willReturn(Optional.of(existing));
        given(topicRepository.findById(1L)).willReturn(Optional.of(topic));
        given(speechRepository.findAiReportSourceSpeeches(10L)).willReturn(List.of(speech));

        AiReportGenerationContext context = aiReportPersistenceService.prepareGeneration(10L, customPrompts);

        String requestJson = new ObjectMapper().findAndRegisterModules().writeValueAsString(context.request());

        assertThat(context.shouldCallAi()).isTrue();
        assertThat(context.generationType()).isEqualTo(AiReportGenerationType.CUSTOM_ONLY);
        assertThat(context.reportId()).isEqualTo(55L);
        assertThat(requestJson).contains("\"coreLine\":\"기본 핵심 한줄\"");
        assertThat(requestJson).contains("\"aiSummary\":\"기본 종합 정리\"");
        assertThat(requestJson).contains("\"customPrompts\":[{\"label\":\"custom 1\",\"prompt\":\"표현의 자유를 반박하는 의견 정리해줘\"}]");
        verify(aiReportRepository, never()).save(any());
    }

    @Test
    void prepareGeneration_returnsCompletedReportWithoutAiCall_whenBaseReportAlreadyCompletedAndNoCustomPrompts() {
        Room room = closedRoom(10L, 1L, "room title");
        AiReport existing = AiReport.pending(10L);
        ReflectionTestUtils.setField(existing, "id", 55L);
        existing.complete(new AiReportGenerateRes(
                "core",
                List.of("issue"),
                "summary",
                "common",
                "opinion"
        ));

        given(roomRepository.findByIdForUpdate(10L)).willReturn(Optional.of(room));
        given(aiReportRepository.findByRoomIdForUpdate(10L)).willReturn(Optional.of(existing));

        AiReportGenerationContext context = aiReportPersistenceService.prepareGeneration(10L, List.of());

        assertThat(context.shouldCallAi()).isFalse();
        assertThat(context.response().status()).isEqualTo("COMPLETED");
        assertThat(context.response().coreLine()).isEqualTo("core");
        verify(topicRepository, never()).findById(any());
        verify(speechRepository, never()).findAiReportSourceSpeeches(any());
        verify(aiReportRepository, never()).save(any());
    }

    @Test
    void prepareGeneration_returnsExistingReportWithoutAiCall_whenReportIsPendingOrCompleted() {
        Room room = closedRoom(10L, 1L, "토론방 제목");
        AiReport existing = AiReport.pending(10L);
        ReflectionTestUtils.setField(existing, "id", 55L);

        given(roomRepository.findByIdForUpdate(10L)).willReturn(Optional.of(room));
        given(aiReportRepository.findByRoomIdForUpdate(10L)).willReturn(Optional.of(existing));

        AiReportGenerationContext context = aiReportPersistenceService.prepareGeneration(10L, List.of());

        assertThat(context.shouldCallAi()).isFalse();
        assertThat(context.response().status()).isEqualTo("REQUESTED");
        verify(topicRepository, never()).findById(any());
        verify(speechRepository, never()).findAiReportSourceSpeeches(any());
    }

    @Test
    void prepareGeneration_throwsRoomNotClosed_whenRoomIsOpen() {
        Room room = Room.open(
                1L,
                "진행 중인 토론방",
                LocalDateTime.of(2026, 6, 22, 10, 0),
                LocalDateTime.of(2026, 6, 22, 13, 0),
                10
        );
        ReflectionTestUtils.setField(room, "id", 10L);

        given(roomRepository.findByIdForUpdate(10L)).willReturn(Optional.of(room));

        assertThatThrownBy(() -> aiReportPersistenceService.prepareGeneration(10L, List.of()))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.AI_REPORT_ROOM_NOT_CLOSED);

        verify(aiReportRepository, never()).save(any());
    }

    @Test
    void requestGeneration_throwsRoomNotFound_whenRoomDoesNotExist() {
        given(roomRepository.findByIdForUpdate(10L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> aiReportPersistenceService.requestGeneration(10L, 7L, List.of()))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ROOM_NOT_FOUND);
    }

    @Test
    void requestGeneration_throwsRoomNotClosed_whenRoomIsOpen() {
        Room room = Room.open(
                1L,
                "진행 중인 토론방",
                LocalDateTime.of(2026, 6, 22, 10, 0),
                LocalDateTime.of(2026, 6, 22, 13, 0),
                10
        );
        ReflectionTestUtils.setField(room, "id", 10L);
        given(roomRepository.findByIdForUpdate(10L)).willReturn(Optional.of(room));

        assertThatThrownBy(() -> aiReportPersistenceService.requestGeneration(10L, 7L, List.of()))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.AI_REPORT_ROOM_NOT_CLOSED);
    }

    @Test
    void requestGeneration_skipsPublish_whenExistingReportIsInProgress() {
        Room room = closedRoom(10L, 1L, "room title");
        AiReport existing = AiReport.pending(10L);
        ReflectionTestUtils.setField(existing, "id", 55L);

        given(roomRepository.findByIdForUpdate(10L)).willReturn(Optional.of(room));
        given(aiReportRepository.findByRoomIdForUpdate(10L)).willReturn(Optional.of(existing));

        AiReportRequestResult result = aiReportPersistenceService.requestGeneration(10L, 7L, List.of());

        assertThat(result.shouldPublish()).isFalse();
        assertThat(result.generationType()).isEqualTo(AiReportGenerationType.SKIP);
        assertThat(result.response().status()).isEqualTo("REQUESTED");
        verify(aiReportRepository, never()).save(any());
    }

    @Test
    void requestGeneration_skipsPublish_whenCompletedReportExistsAndCustomPromptsAreEmpty() {
        Room room = closedRoom(10L, 1L, "room title");
        AiReport existing = completedAiReport(10L);
        ReflectionTestUtils.setField(existing, "id", 55L);

        given(roomRepository.findByIdForUpdate(10L)).willReturn(Optional.of(room));
        given(aiReportRepository.findByRoomIdForUpdate(10L)).willReturn(Optional.of(existing));

        AiReportRequestResult result = aiReportPersistenceService.requestGeneration(10L, 7L, List.of());

        assertThat(result.shouldPublish()).isFalse();
        assertThat(result.generationType()).isEqualTo(AiReportGenerationType.SKIP);
        assertThat(result.response().status()).isEqualTo("COMPLETED");
    }

    @Test
    void requestGeneration_requestsCustomOnly_whenCompletedReportExistsAndCustomPromptsExist() {
        Room room = closedRoom(10L, 1L, "room title");
        AiReport existing = completedAiReport(10L);
        ReflectionTestUtils.setField(existing, "id", 55L);
        List<CustomPromptCommand> customPrompts = List.of(
                new CustomPromptCommand("custom 1", "개인화 질문")
        );

        given(roomRepository.findByIdForUpdate(10L)).willReturn(Optional.of(room));
        given(aiReportRepository.findByRoomIdForUpdate(10L)).willReturn(Optional.of(existing));

        AiReportRequestResult result = aiReportPersistenceService.requestGeneration(10L, 7L, customPrompts);

        assertThat(result.shouldPublish()).isTrue();
        assertThat(result.generationType()).isEqualTo(AiReportGenerationType.CUSTOM_ONLY);
        assertThat(existing.getStatus()).isEqualTo(AiReportStatus.REQUESTED);
        assertThat(existing.getCustomPrompts()).containsExactly(
                new AiReportCustomPrompt(7L, "custom 1", "개인화 질문")
        );
    }

    @Test
    void requestGeneration_savesNewReportAsBaseOnly_whenReportDoesNotExistAndCustomPromptsAreNull() {
        Room room = closedRoom(10L, 1L, "room title");
        given(roomRepository.findByIdForUpdate(10L)).willReturn(Optional.of(room));
        given(aiReportRepository.findByRoomIdForUpdate(10L)).willReturn(Optional.empty());
        given(aiReportRepository.save(any(AiReport.class))).willAnswer(invocation -> {
            AiReport report = invocation.getArgument(0);
            ReflectionTestUtils.setField(report, "id", 55L);
            return report;
        });

        AiReportRequestResult result = aiReportPersistenceService.requestGeneration(10L, 7L, null);

        assertThat(result.shouldPublish()).isTrue();
        assertThat(result.generationType()).isEqualTo(AiReportGenerationType.BASE_ONLY);
        assertThat(result.response().status()).isEqualTo("REQUESTED");
    }

    @Test
    void requestGeneration_retriesExistingFailedReportAsBaseWithCustom() {
        Room room = closedRoom(10L, 1L, "room title");
        AiReport existing = AiReport.pending(10L);
        existing.fail("failed");
        ReflectionTestUtils.setField(existing, "id", 55L);
        List<CustomPromptCommand> customPrompts = List.of(
                new CustomPromptCommand("custom 1", "개인화 질문")
        );

        given(roomRepository.findByIdForUpdate(10L)).willReturn(Optional.of(room));
        given(aiReportRepository.findByRoomIdForUpdate(10L)).willReturn(Optional.of(existing));

        AiReportRequestResult result = aiReportPersistenceService.requestGeneration(10L, 7L, customPrompts);

        assertThat(result.shouldPublish()).isTrue();
        assertThat(result.generationType()).isEqualTo(AiReportGenerationType.BASE_WITH_CUSTOM);
        assertThat(existing.getStatus()).isEqualTo(AiReportStatus.REQUESTED);
        assertThat(existing.getCustomPrompts()).containsExactly(
                new AiReportCustomPrompt(7L, "custom 1", "개인화 질문")
        );
    }

    @Test
    void requestBaseGenerationFromRoomClose_createsBaseReport_whenReportDoesNotExist() {
        Room room = closedRoom(10L, 1L, "room title");
        given(roomRepository.findByIdForUpdate(10L)).willReturn(Optional.of(room));
        given(aiReportRepository.findByRoomIdForUpdate(10L)).willReturn(Optional.empty());
        given(aiReportRepository.save(any(AiReport.class))).willAnswer(invocation -> {
            AiReport report = invocation.getArgument(0);
            ReflectionTestUtils.setField(report, "id", 55L);
            return report;
        });

        AiReportRequestResult result = aiReportPersistenceService.requestBaseGenerationFromRoomClose(10L);

        assertThat(result.shouldPublish()).isTrue();
        assertThat(result.generationType()).isEqualTo(AiReportGenerationType.BASE_ONLY);
    }

    @Test
    void requestBaseGenerationFromRoomClose_retriesFailedReport() {
        Room room = closedRoom(10L, 1L, "room title");
        AiReport existing = AiReport.pending(10L);
        existing.markPublishFailed("PUBLISH_ERROR", "failed");
        ReflectionTestUtils.setField(existing, "id", 55L);

        given(roomRepository.findByIdForUpdate(10L)).willReturn(Optional.of(room));
        given(aiReportRepository.findByRoomIdForUpdate(10L)).willReturn(Optional.of(existing));

        AiReportRequestResult result = aiReportPersistenceService.requestBaseGenerationFromRoomClose(10L);

        assertThat(result.shouldPublish()).isTrue();
        assertThat(result.generationType()).isEqualTo(AiReportGenerationType.BASE_ONLY);
        assertThat(existing.getStatus()).isEqualTo(AiReportStatus.REQUESTED);
    }

    @Test
    void requestBaseGenerationFromRoomClose_skips_whenReportIsCompleted() {
        Room room = closedRoom(10L, 1L, "room title");
        AiReport existing = completedAiReport(10L);
        ReflectionTestUtils.setField(existing, "id", 55L);

        given(roomRepository.findByIdForUpdate(10L)).willReturn(Optional.of(room));
        given(aiReportRepository.findByRoomIdForUpdate(10L)).willReturn(Optional.of(existing));

        AiReportRequestResult result = aiReportPersistenceService.requestBaseGenerationFromRoomClose(10L);

        assertThat(result.shouldPublish()).isFalse();
        assertThat(result.generationType()).isEqualTo(AiReportGenerationType.SKIP);
    }

    @Test
    void requestCustomGeneration_throwsRequired_whenCustomPromptsAreEmpty() {
        assertThatThrownBy(() -> aiReportPersistenceService.requestCustomGeneration(10L, 7L, List.of()))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.AI_REPORT_CUSTOM_PROMPT_REQUIRED);
    }

    @Test
    void requestCustomGeneration_throwsNotFound_whenReportDoesNotExist() {
        Room room = closedRoom(10L, 1L, "room title");
        given(roomRepository.findByIdForUpdate(10L)).willReturn(Optional.of(room));
        given(aiReportRepository.findByRoomIdForUpdate(10L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> aiReportPersistenceService.requestCustomGeneration(
                10L,
                7L,
                List.of(new CustomPromptCommand("custom 1", "개인화 질문"))
        ))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.AI_REPORT_NOT_FOUND);
    }

    @Test
    void requestCustomGeneration_remembersPromptsAndPublishes_whenReportIsInProgress() {
        Room room = closedRoom(10L, 1L, "room title");
        AiReport existing = AiReport.pending(10L);
        ReflectionTestUtils.setField(existing, "id", 55L);
        List<CustomPromptCommand> customPrompts = List.of(
                new CustomPromptCommand("custom 1", "개인화 질문")
        );

        given(roomRepository.findByIdForUpdate(10L)).willReturn(Optional.of(room));
        given(aiReportRepository.findByRoomIdForUpdate(10L)).willReturn(Optional.of(existing));

        AiReportRequestResult result = aiReportPersistenceService.requestCustomGeneration(10L, 7L, customPrompts);

        assertThat(result.shouldPublish()).isTrue();
        assertThat(result.generationType()).isEqualTo(AiReportGenerationType.CUSTOM_ONLY);
        assertThat(existing.getCustomPrompts()).containsExactly(
                new AiReportCustomPrompt(7L, "custom 1", "개인화 질문")
        );
    }

    @Test
    void requestCustomGeneration_throwsAlreadyExists_whenReportStatusCannotRequestCustom() {
        Room room = closedRoom(10L, 1L, "room title");
        AiReport existing = AiReport.pending(10L);
        existing.fail("failed");
        ReflectionTestUtils.setField(existing, "id", 55L);
        given(roomRepository.findByIdForUpdate(10L)).willReturn(Optional.of(room));
        given(aiReportRepository.findByRoomIdForUpdate(10L)).willReturn(Optional.of(existing));

        assertThatThrownBy(() -> aiReportPersistenceService.requestCustomGeneration(
                10L,
                7L,
                List.of(new CustomPromptCommand("custom 1", "개인화 질문"))
        ))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.AI_REPORT_ALREADY_EXISTS);
    }

    @Test
    void requestCustomGeneration_requestsCustomOnly_whenReportIsPublishFailed() {
        Room room = closedRoom(10L, 1L, "room title");
        AiReport existing = completedAiReport(10L);
        existing.markPublishFailed("PUBLISH_ERROR", "failed");
        ReflectionTestUtils.setField(existing, "id", 55L);
        List<CustomPromptCommand> customPrompts = List.of(
                new CustomPromptCommand("custom 1", "개인화 질문")
        );

        given(roomRepository.findByIdForUpdate(10L)).willReturn(Optional.of(room));
        given(aiReportRepository.findByRoomIdForUpdate(10L)).willReturn(Optional.of(existing));

        AiReportRequestResult result = aiReportPersistenceService.requestCustomGeneration(10L, 7L, customPrompts);

        assertThat(result.shouldPublish()).isTrue();
        assertThat(result.generationType()).isEqualTo(AiReportGenerationType.CUSTOM_ONLY);
        assertThat(existing.getStatus()).isEqualTo(AiReportStatus.REQUESTED);
    }

    @Test
    void complete_savesCompletedReportFields() {
        AiReport report = AiReport.pending(10L, List.of(new AiReportCustomPrompt(
                "custom 1",
                "표현의 자유를 반박하는 의견 정리해줘"
        )));
        ReflectionTestUtils.setField(report, "id", 55L);
        given(aiReportRepository.findById(55L)).willReturn(Optional.of(report));

        AiReportRes result = aiReportPersistenceService.complete(55L, new AiReportGenerateRes(
                "핵심 한줄",
                List.of("쟁점 1", "쟁점 2"),
                "종합 정리",
                "공통 의견",
                "개인적 소견",
                List.of(new AiReportCustomReportPayload("표현의 자유 주장 반박 의견", "반박 내용"))
        ));

        assertThat(report.getStatus()).isEqualTo(AiReportStatus.COMPLETED);
        assertThat(report.getCoreLine()).isEqualTo("핵심 한줄");
        assertThat(report.getKeyIssues()).containsExactly("쟁점 1", "쟁점 2");
        assertThat(report.getAiSummary()).isEqualTo("종합 정리");
        assertThat(report.getCommonGround()).isEqualTo("공통 의견");
        assertThat(report.getAiOpinion()).isEqualTo("개인적 소견");
        assertThat(report.getCustomReports()).containsExactly(new AiReportCustomReport(
                "custom 1",
                "표현의 자유를 반박하는 의견 정리해줘",
                "표현의 자유 주장 반박 의견",
                "반박 내용"
        ));
        assertThat(report.getCompletedAt()).isNotNull();
        assertThat(report.getErrorMessage()).isNull();
        assertThat(result.status()).isEqualTo("COMPLETED");
        assertThat(result.customReports()).hasSize(1);
    }

    @Test
    void appendCustomReports_keepsBaseReportCompletedAndAddsCustomResults() {
        AiReport report = AiReport.pending(10L);
        ReflectionTestUtils.setField(report, "id", 55L);
        report.complete(new AiReportGenerateRes(
                "기본 핵심 한줄",
                List.of("기본 쟁점"),
                "기본 종합 정리",
                "기본 공통 의견",
                "기본 개인 소견",
                List.of()
        ));
        List<CustomPromptCommand> prompts = List.of(
                new CustomPromptCommand("custom 1", "소수 의견도 정리해줘")
        );

        given(aiReportRepository.findById(55L)).willReturn(Optional.of(report));

        AiReportRes result = aiReportPersistenceService.appendCustomReports(
                55L,
                prompts,
                List.of(new AiReportCustomReportPayload("소수 의견", "소수 의견 내용"))
        );

        assertThat(report.getStatus()).isEqualTo(AiReportStatus.COMPLETED);
        assertThat(report.getCoreLine()).isEqualTo("기본 핵심 한줄");
        assertThat(report.getCustomReports()).containsExactly(new AiReportCustomReport(
                "custom 1",
                "소수 의견도 정리해줘",
                "소수 의견",
                "소수 의견 내용"
        ));
        assertThat(result.customReports()).hasSize(1);
    }

    @Test
    void appendCustomReports_returnsOnlyCustomResultsForCurrentUser() {
        AiReport report = AiReport.pending(10L);
        ReflectionTestUtils.setField(report, "id", 55L);
        report.complete(new AiReportGenerateRes(
                "core",
                List.of("issue"),
                "summary",
                "common",
                "opinion",
                List.of()
        ));
        List<CustomPromptCommand> prompts = List.of(new CustomPromptCommand("custom 1", "minority view"));

        given(aiReportRepository.findById(55L)).willReturn(Optional.of(report));

        aiReportPersistenceService.appendCustomReports(
                55L,
                7L,
                prompts,
                List.of(new AiReportCustomReportPayload("user seven", "only user seven"))
        );
        AiReportRes result = aiReportPersistenceService.appendCustomReports(
                55L,
                8L,
                prompts,
                List.of(new AiReportCustomReportPayload("user eight", "only user eight"))
        );

        assertThat(report.getCustomReports()).containsExactly(
                new AiReportCustomReport(7L, "custom 1", "minority view", "user seven", "only user seven"),
                new AiReportCustomReport(8L, "custom 1", "minority view", "user eight", "only user eight")
        );
        assertThat(result.customReports()).containsExactly(new AiReportRes.CustomReportRes(
                "custom 1",
                "minority view",
                "user eight",
                "only user eight"
        ));
    }

    @Test
    void getReport_returnsViewerPdfStatus_whenExportExists() {
        AiReport report = AiReport.pending(10L);
        ReflectionTestUtils.setField(report, "id", 55L);
        report.complete(new AiReportGenerateRes(
                "core",
                List.of("issue"),
                "summary",
                "common",
                "opinion"
        ));
        AiReportPdfExport export = AiReportPdfExport.notStarted(55L, 10L, 7L);
        ReflectionTestUtils.setField(export, "id", 99L);

        given(aiReportRepository.findByRoomId(10L)).willReturn(Optional.of(report));
        given(aiReportPdfExportRepository.findByAiReportIdAndRequestedByUserIdAndPdfType(55L, 7L, AiReportPdfType.BASE))
                .willReturn(Optional.of(export));

        AiReportRes result = aiReportPersistenceService.getReport(10L, 7L);

        assertThat(result.pdf().pdfExportId()).isEqualTo(99L);
        assertThat(result.pdf().pdfStatus()).isEqualTo("NOT_STARTED");
        assertThat(result.pdf().downloadAvailable()).isFalse();
    }

    @Test
    void fail_savesFailedStatusAndSafeErrorMessage() {
        AiReport report = AiReport.pending(10L);
        ReflectionTestUtils.setField(report, "id", 55L);
        given(aiReportRepository.findById(55L)).willReturn(Optional.of(report));

        AiReportRes result = aiReportPersistenceService.fail(55L, "AI 리포트 생성에 실패했습니다.");

        assertThat(report.getStatus()).isEqualTo(AiReportStatus.GENERATION_FAILED);
        assertThat(report.getErrorMessage()).isEqualTo("AI 리포트 생성에 실패했습니다.");
        assertThat(report.getCompletedAt()).isNull();
        assertThat(result.status()).isEqualTo("GENERATION_FAILED");
    }

    private Room closedRoom(Long roomId, Long topicId, String title) {
        Room room = Room.open(
                topicId,
                title,
                LocalDateTime.of(2026, 6, 22, 10, 0),
                LocalDateTime.of(2026, 6, 22, 13, 0),
                10
        );
        ReflectionTestUtils.setField(room, "id", roomId);
        room.close(LocalDateTime.of(2026, 6, 22, 13, 0));
        assertThat(room.getStatus()).isEqualTo(RoomStatus.CLOSED);
        return room;
    }

    private AiReport completedAiReport(Long roomId) {
        AiReport report = AiReport.pending(roomId);
        report.complete(new AiReportGenerateRes(
                "core",
                List.of("issue"),
                "summary",
                "common",
                "opinion"
        ));
        return report;
    }

    private Speech completedSpeech(
            Long speechId,
            Long roomId,
            Long userId,
            String content,
            SpeechStance stance,
            LocalDateTime createdAt
    ) {
        Speech speech = Speech.createMainOpinion(roomId, userId, content, stance);
        ReflectionTestUtils.setField(speech, "id", speechId);
        ReflectionTestUtils.setField(speech, "status", SpeechStatus.COMPLETED);
        ReflectionTestUtils.setField(speech, "createdAt", createdAt.plusSeconds(10));
        ReflectionTestUtils.setField(speech, "startedAt", createdAt);
        ReflectionTestUtils.setField(speech, "endedAt", createdAt.plusMinutes(3));
        return speech;
    }
}
