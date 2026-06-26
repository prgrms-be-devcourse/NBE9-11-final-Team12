package com.sisibibi.api.domain.roomparticipant.service;

import com.sisibibi.api.domain.roomparticipant.dto.event.RoomParticipantChangedEvent;
import com.sisibibi.api.domain.roomparticipant.dto.event.RoomParticipantEventType;
import com.sisibibi.api.domain.roomparticipant.entity.RoomParticipant;
import com.sisibibi.api.domain.roomparticipant.entity.RoomParticipantStatus;
import com.sisibibi.api.domain.roomparticipant.repository.RoomParticipantRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class RoomParticipantForceLeaveServiceTest {

    @Mock
    private RoomParticipantRepository roomParticipantRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private RoomParticipantForceLeaveService forceLeaveService;

    @Test
    void leaveAllJoinedRooms_marksJoinedParticipantsLeftAndPublishesEvents() {
        RoomParticipant first = participant(1L, 10L);
        RoomParticipant second = participant(2L, 10L);
        given(roomParticipantRepository.findByUserIdAndStatus(
                10L,
                RoomParticipantStatus.JOINED
        )).willReturn(List.of(first, second));
        given(roomParticipantRepository.countByRoomIdAndStatus(
                1L,
                RoomParticipantStatus.JOINED
        )).willReturn(3);
        given(roomParticipantRepository.countByRoomIdAndStatus(
                2L,
                RoomParticipantStatus.JOINED
        )).willReturn(5);

        int leftCount = forceLeaveService.leaveAllJoinedRooms(10L);

        assertThat(leftCount).isEqualTo(2);
        assertThat(first.getStatus()).isEqualTo(RoomParticipantStatus.LEFT);
        assertThat(second.getStatus()).isEqualTo(RoomParticipantStatus.LEFT);

        ArgumentCaptor<RoomParticipantChangedEvent> eventCaptor =
                ArgumentCaptor.forClass(RoomParticipantChangedEvent.class);
        verify(eventPublisher, org.mockito.Mockito.times(2))
                .publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getAllValues())
                .extracting(RoomParticipantChangedEvent::type)
                .containsOnly(RoomParticipantEventType.PARTICIPANT_LEFT);
        assertThat(eventCaptor.getAllValues())
                .extracting(RoomParticipantChangedEvent::roomId)
                .containsExactly(1L, 2L);
        assertThat(eventCaptor.getAllValues())
                .extracting(event -> event.payload().participantCount())
                .containsExactly(3, 5);
    }

    @Test
    void leaveAllJoinedRooms_returnsZero_whenNoJoinedParticipantExists() {
        given(roomParticipantRepository.findByUserIdAndStatus(
                10L,
                RoomParticipantStatus.JOINED
        )).willReturn(List.of());

        int leftCount = forceLeaveService.leaveAllJoinedRooms(10L);

        assertThat(leftCount).isZero();
        verifyNoInteractions(eventPublisher);
    }

    private RoomParticipant participant(Long roomId, Long userId) {
        RoomParticipant participant = RoomParticipant.join(roomId, userId);
        ReflectionTestUtils.setField(participant, "id", roomId * 100);
        return participant;
    }
}
