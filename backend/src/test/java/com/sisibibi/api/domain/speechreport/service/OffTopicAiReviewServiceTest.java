package com.sisibibi.api.domain.speechreport.service;

import com.sisibibi.api.domain.room.entity.Room;
import com.sisibibi.api.domain.room.repository.RoomRepository;
import com.sisibibi.api.domain.roomparticipant.entity.RoomParticipantStatus;
import com.sisibibi.api.domain.roomparticipant.repository.RoomParticipantRepository;
import com.sisibibi.api.domain.speech.entity.SpeechDeleteReason;
import com.sisibibi.api.domain.speech.entity.Speech;
import com.sisibibi.api.domain.speech.entity.SpeechStance;
import com.sisibibi.api.domain.speech.repository.SpeechRepository;
import com.sisibibi.api.domain.speechreport.config.OffTopicAiReviewProperties;
import com.sisibibi.api.domain.speechreport.entity.OffTopicAiReview;
import com.sisibibi.api.domain.speechreport.entity.OffTopicAiReviewStatus;
import com.sisibibi.api.domain.speechreport.entity.SpeechReportReason;
import com.sisibibi.api.domain.speechreport.repository.OffTopicAiReviewRepository;
import com.sisibibi.api.domain.speechreport.repository.SpeechReportRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.Executor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OffTopicAiReviewServiceTest {

    @Mock
    private SpeechReportRepository speechReportRepository;

    @Mock
    private OffTopicAiReviewRepository offTopicAiReviewRepository;

    @Mock
    private RoomParticipantRepository roomParticipantRepository;

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private SpeechRepository speechRepository;

    @Mock
    private OffTopicAiReviewer offTopicAiReviewer;

    private OffTopicAiReviewService offTopicAiReviewService;

    @BeforeEach
    void setUp() {
        OffTopicAiReviewProperties properties = new OffTopicAiReviewProperties();
        properties.setGenerateTimeout(Duration.ofSeconds(1));
        Executor directExecutor = Runnable::run;
        offTopicAiReviewService = new OffTopicAiReviewService(
                speechReportRepository,
                offTopicAiReviewRepository,
                roomParticipantRepository,
                roomRepository,
                speechRepository,
                offTopicAiReviewer,
                directExecutor,
                properties
        );
    }

    @Test
    void threshold_usesMinimumFiveUntilJoinedCountIsBelowSixty() {
        assertThat(offTopicAiReviewService.calculateThreshold(1)).isEqualTo(5);
        assertThat(offTopicAiReviewService.calculateThreshold(59)).isEqualTo(5);
        assertThat(offTopicAiReviewService.calculateThreshold(60)).isEqualTo(6);
        assertThat(offTopicAiReviewService.calculateThreshold(70)).isEqualTo(7);
    }

    @Test
    void triggerIfNeeded_skipsWhenReasonIsNotOffTopic() {
        Speech speech = speech(10L, 1L, "opinion");

        offTopicAiReviewService.triggerIfNeeded(speech, SpeechReportReason.SPAM);

        verify(offTopicAiReviewRepository, never()).save(
                org.mockito.ArgumentMatchers.any(OffTopicAiReview.class)
        );
    }

    @Test
    void triggerIfNeeded_skipsWhenOffTopicReportCountIsBelowThreshold() {
        Speech speech = speech(10L, 1L, "opinion");
        given(roomParticipantRepository.countByRoomIdAndStatus(1L, RoomParticipantStatus.JOINED))
                .willReturn(50);
        given(speechReportRepository.countBySpeechIdAndReason(
                10L,
                SpeechReportReason.OFF_TOPIC
        )).willReturn(4);

        offTopicAiReviewService.triggerIfNeeded(speech, SpeechReportReason.OFF_TOPIC);

        verify(offTopicAiReviewRepository, never()).save(
                org.mockito.ArgumentMatchers.any(OffTopicAiReview.class)
        );
    }

    @Test
    void triggerIfNeeded_createsPendingReviewAndCompletesAiResultWhenThresholdIsReached() {
        Speech speech = speech(10L, 1L, "unrelated food story");
        Room room = Room.open(100L, "Should school uniforms be mandatory?", null, null, 50);
        given(roomParticipantRepository.countByRoomIdAndStatus(1L, RoomParticipantStatus.JOINED))
                .willReturn(50);
        given(speechReportRepository.countBySpeechIdAndReason(
                10L,
                SpeechReportReason.OFF_TOPIC
        )).willReturn(5);
        given(offTopicAiReviewRepository.findBySpeechId(10L)).willReturn(Optional.empty());
        given(roomRepository.findById(1L)).willReturn(Optional.of(room));
        given(offTopicAiReviewRepository.save(org.mockito.ArgumentMatchers.any(OffTopicAiReview.class)))
                .willAnswer(invocation -> {
                    OffTopicAiReview review = invocation.getArgument(0);
                    ReflectionTestUtils.setField(review, "id", 100L);
                    return review;
                });
        given(offTopicAiReviewer.review(speech, "Should school uniforms be mandatory?"))
                .willReturn(new OffTopicAiReviewResult(
                        true,
                        "The speech is unrelated to the room title.",
                        0.82
                ));

        offTopicAiReviewService.triggerIfNeeded(speech, SpeechReportReason.OFF_TOPIC);

        ArgumentCaptor<OffTopicAiReview> reviewCaptor =
                ArgumentCaptor.forClass(OffTopicAiReview.class);
        verify(offTopicAiReviewRepository, times(2)).save(reviewCaptor.capture());
        OffTopicAiReview review = reviewCaptor.getAllValues().getLast();
        assertThat(review.getSpeechId()).isEqualTo(10L);
        assertThat(review.getRoomId()).isEqualTo(1L);
        assertThat(review.getReportCount()).isEqualTo(5);
        assertThat(review.getThreshold()).isEqualTo(5);
        assertThat(review.getParticipantCount()).isEqualTo(50);
        assertThat(review.getStatus()).isEqualTo(OffTopicAiReviewStatus.COMPLETED);
        assertThat(review.isOffTopic()).isTrue();
        assertThat(review.getReason()).isEqualTo("The speech is unrelated to the room title.");
        verify(offTopicAiReviewer).review(speech, "Should school uniforms be mandatory?");
    }

    @Test
    void triggerIfNeeded_softDeletesSpeechWhenRelevanceScoreIsBelowAutoDeleteThreshold() {
        Speech speech = speech(10L, 1L, "buy cheap pills now");
        Room room = Room.open(100L, "Should school uniforms be mandatory?", null, null, 50);
        given(roomParticipantRepository.countByRoomIdAndStatus(1L, RoomParticipantStatus.JOINED))
                .willReturn(50);
        given(speechReportRepository.countBySpeechIdAndReason(
                10L,
                SpeechReportReason.OFF_TOPIC
        )).willReturn(5);
        given(offTopicAiReviewRepository.findBySpeechId(10L)).willReturn(Optional.empty());
        given(roomRepository.findById(1L)).willReturn(Optional.of(room));
        given(speechRepository.findByIdAndDeletedFalse(10L)).willReturn(Optional.of(speech));
        given(offTopicAiReviewRepository.save(org.mockito.ArgumentMatchers.any(OffTopicAiReview.class)))
                .willAnswer(invocation -> {
                    OffTopicAiReview review = invocation.getArgument(0);
                    ReflectionTestUtils.setField(review, "id", 100L);
                    return review;
                });
        given(offTopicAiReviewer.review(speech, "Should school uniforms be mandatory?"))
                .willReturn(new OffTopicAiReviewResult(
                        true,
                        "The speech has very low relevance to the room title.",
                        0.2
                ));

        offTopicAiReviewService.triggerIfNeeded(speech, SpeechReportReason.OFF_TOPIC);

        assertThat(speech.isDeleted()).isTrue();
        assertThat(speech.getDeleteReason()).isEqualTo(SpeechDeleteReason.OFF_TOPIC);
        verify(speechRepository).save(speech);
    }

    @Test
    void triggerIfNeeded_doesNotSoftDeleteSpeechWhenRelevanceScoreIsSuspiciousButNotLowEnough() {
        Speech speech = speech(10L, 1L, "somewhat related opinion");
        Room room = Room.open(100L, "Should school uniforms be mandatory?", null, null, 50);
        given(roomParticipantRepository.countByRoomIdAndStatus(1L, RoomParticipantStatus.JOINED))
                .willReturn(50);
        given(speechReportRepository.countBySpeechIdAndReason(
                10L,
                SpeechReportReason.OFF_TOPIC
        )).willReturn(5);
        given(offTopicAiReviewRepository.findBySpeechId(10L)).willReturn(Optional.empty());
        given(roomRepository.findById(1L)).willReturn(Optional.of(room));
        given(offTopicAiReviewRepository.save(org.mockito.ArgumentMatchers.any(OffTopicAiReview.class)))
                .willAnswer(invocation -> {
                    OffTopicAiReview review = invocation.getArgument(0);
                    ReflectionTestUtils.setField(review, "id", 100L);
                    return review;
                });
        given(offTopicAiReviewer.review(speech, "Should school uniforms be mandatory?"))
                .willReturn(new OffTopicAiReviewResult(
                        false,
                        "The speech is suspicious but not clearly unrelated.",
                        0.45
                ));

        offTopicAiReviewService.triggerIfNeeded(speech, SpeechReportReason.OFF_TOPIC);

        assertThat(speech.isDeleted()).isFalse();
        verify(speechRepository, never()).save(org.mockito.ArgumentMatchers.any(Speech.class));
    }

    @Test
    void triggerIfNeeded_skipsWhenReviewAlreadyExists() {
        Speech speech = speech(10L, 1L, "opinion");
        OffTopicAiReview existing = OffTopicAiReview.pending(
                10L,
                1L,
                "opinion",
                5,
                5,
                50
        );
        given(roomParticipantRepository.countByRoomIdAndStatus(1L, RoomParticipantStatus.JOINED))
                .willReturn(50);
        given(speechReportRepository.countBySpeechIdAndReason(
                10L,
                SpeechReportReason.OFF_TOPIC
        )).willReturn(5);
        given(offTopicAiReviewRepository.findBySpeechId(10L)).willReturn(Optional.of(existing));

        offTopicAiReviewService.triggerIfNeeded(speech, SpeechReportReason.OFF_TOPIC);

        verify(offTopicAiReviewer, never()).review(
                org.mockito.ArgumentMatchers.eq(speech),
                org.mockito.ArgumentMatchers.anyString()
        );
        verify(offTopicAiReviewRepository, never()).save(
                org.mockito.ArgumentMatchers.any(OffTopicAiReview.class)
        );
    }

    @Test
    void triggerIfNeeded_marksReviewFailedWhenAiResultIsInvalid() {
        Speech speech = speech(10L, 1L, "opinion");
        Room room = Room.open(100L, "Should school uniforms be mandatory?", null, null, 50);
        given(roomParticipantRepository.countByRoomIdAndStatus(1L, RoomParticipantStatus.JOINED))
                .willReturn(50);
        given(speechReportRepository.countBySpeechIdAndReason(
                10L,
                SpeechReportReason.OFF_TOPIC
        )).willReturn(5);
        given(offTopicAiReviewRepository.findBySpeechId(10L)).willReturn(Optional.empty());
        given(roomRepository.findById(1L)).willReturn(Optional.of(room));
        given(offTopicAiReviewRepository.save(org.mockito.ArgumentMatchers.any(OffTopicAiReview.class)))
                .willAnswer(invocation -> {
                    OffTopicAiReview review = invocation.getArgument(0);
                    ReflectionTestUtils.setField(review, "id", 100L);
                    return review;
                });
        given(offTopicAiReviewer.review(speech, "Should school uniforms be mandatory?"))
                .willReturn(new OffTopicAiReviewResult(true, " ", 0.8));

        offTopicAiReviewService.triggerIfNeeded(speech, SpeechReportReason.OFF_TOPIC);

        ArgumentCaptor<OffTopicAiReview> reviewCaptor =
                ArgumentCaptor.forClass(OffTopicAiReview.class);
        verify(offTopicAiReviewRepository, times(2)).save(reviewCaptor.capture());
        OffTopicAiReview review = reviewCaptor.getAllValues().getLast();
        assertThat(review.getStatus()).isEqualTo(OffTopicAiReviewStatus.FAILED);
        assertThat(review.getErrorMessage()).contains("reason is blank");
    }

    private Speech speech(Long speechId, Long roomId, String content) {
        Speech speech = Speech.createMainOpinion(roomId, 20L, content, SpeechStance.PRO);
        ReflectionTestUtils.setField(speech, "id", speechId);
        return speech;
    }
}
