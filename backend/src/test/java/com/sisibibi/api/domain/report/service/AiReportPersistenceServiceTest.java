package com.sisibibi.api.domain.report.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sisibibi.api.domain.report.client.dto.AiReportGenerateReq;
import com.sisibibi.api.domain.report.client.dto.AiReportGenerateRes;
import com.sisibibi.api.domain.report.entity.AiReport;
import com.sisibibi.api.domain.report.entity.AiReportStatus;
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

    @InjectMocks
    private AiReportPersistenceService aiReportPersistenceService;

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

        AiReportGenerationContext context = aiReportPersistenceService.prepareGeneration(10L);

        ArgumentCaptor<AiReport> reportCaptor = ArgumentCaptor.forClass(AiReport.class);
        verify(aiReportRepository).save(reportCaptor.capture());
        verify(speechRepository).findAiReportSourceSpeeches(10L);

        AiReportGenerateReq request = context.request();
        String requestJson = new ObjectMapper().findAndRegisterModules().writeValueAsString(request);

        assertThat(context.shouldCallAi()).isTrue();
        assertThat(context.reportId()).isEqualTo(55L);
        assertThat(reportCaptor.getValue().getRoomId()).isEqualTo(10L);
        assertThat(requestJson).contains("\"title\":\"토론방 제목\"");
        assertThat(requestJson).contains("\"title\":\"토픽 제목\"");
        assertThat(requestJson).contains("\"speechId\":100");
        assertThat(requestJson).contains("\"userId\":7");
        assertThat(requestJson).contains("\"stance\":\"PRO\"");
        assertThat(requestJson).contains("\"content\":\"발언 내용 정리\"");
        assertThat(requestJson).doesNotContain("nickname");
        assertThat(requestJson).doesNotContain("email");
        assertThat(requestJson).doesNotContain("password");
        assertThat(requestJson).doesNotContain("linkUrl");
        assertThat(requestJson).doesNotContain("imageUrl");
        assertThat(requestJson).doesNotContain("updatedAt");
    }

    @Test
    void prepareGeneration_returnsExistingReportWithoutAiCall_whenReportIsPendingOrCompleted() {
        Room room = closedRoom(10L, 1L, "토론방 제목");
        AiReport existing = AiReport.pending(10L);
        ReflectionTestUtils.setField(existing, "id", 55L);

        given(roomRepository.findByIdForUpdate(10L)).willReturn(Optional.of(room));
        given(aiReportRepository.findByRoomIdForUpdate(10L)).willReturn(Optional.of(existing));

        AiReportGenerationContext context = aiReportPersistenceService.prepareGeneration(10L);

        assertThat(context.shouldCallAi()).isFalse();
        assertThat(context.response().status()).isEqualTo("PENDING");
        verify(topicRepository, never()).findById(any());
        verify(speechRepository, never()).findAiReportSourceSpeeches(any());
    }

    @Test
    void prepareGeneration_throwsRoomNotClosed_whenRoomIsOpen() {
        Room room = Room.open(1L, "진행 중인 토론방");
        ReflectionTestUtils.setField(room, "id", 10L);

        given(roomRepository.findByIdForUpdate(10L)).willReturn(Optional.of(room));

        assertThatThrownBy(() -> aiReportPersistenceService.prepareGeneration(10L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.AI_REPORT_ROOM_NOT_CLOSED);

        verify(aiReportRepository, never()).save(any());
    }

    @Test
    void complete_savesCompletedReportFields() {
        AiReport report = AiReport.pending(10L);
        ReflectionTestUtils.setField(report, "id", 55L);
        given(aiReportRepository.findById(55L)).willReturn(Optional.of(report));

        AiReportRes result = aiReportPersistenceService.complete(55L, new AiReportGenerateRes(
                "핵심 한줄",
                List.of("쟁점 1", "쟁점 2"),
                "종합 정리",
                "공통 의견",
                "개인적 소견"
        ));

        assertThat(report.getStatus()).isEqualTo(AiReportStatus.COMPLETED);
        assertThat(report.getCoreLine()).isEqualTo("핵심 한줄");
        assertThat(report.getKeyIssues()).containsExactly("쟁점 1", "쟁점 2");
        assertThat(report.getAiSummary()).isEqualTo("종합 정리");
        assertThat(report.getCommonGround()).isEqualTo("공통 의견");
        assertThat(report.getAiOpinion()).isEqualTo("개인적 소견");
        assertThat(report.getCompletedAt()).isNotNull();
        assertThat(report.getErrorMessage()).isNull();
        assertThat(result.status()).isEqualTo("COMPLETED");
    }

    @Test
    void fail_savesFailedStatusAndSafeErrorMessage() {
        AiReport report = AiReport.pending(10L);
        ReflectionTestUtils.setField(report, "id", 55L);
        given(aiReportRepository.findById(55L)).willReturn(Optional.of(report));

        AiReportRes result = aiReportPersistenceService.fail(55L, "AI 리포트 생성에 실패했습니다.");

        assertThat(report.getStatus()).isEqualTo(AiReportStatus.FAILED);
        assertThat(report.getErrorMessage()).isEqualTo("AI 리포트 생성에 실패했습니다.");
        assertThat(report.getCompletedAt()).isNull();
        assertThat(result.status()).isEqualTo("FAILED");
    }

    private Room closedRoom(Long roomId, Long topicId, String title) {
        Room room = Room.open(topicId, title);
        ReflectionTestUtils.setField(room, "id", roomId);
        ReflectionTestUtils.setField(room, "startedAt", LocalDateTime.of(2026, 6, 22, 10, 0));
        room.close(LocalDateTime.of(2026, 6, 22, 13, 0));
        assertThat(room.getStatus()).isEqualTo(RoomStatus.CLOSED);
        return room;
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
