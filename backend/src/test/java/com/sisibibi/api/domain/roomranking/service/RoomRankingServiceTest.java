package com.sisibibi.api.domain.roomranking.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;

import com.sisibibi.api.domain.chat.repository.ChatMessageRepository;
import com.sisibibi.api.domain.room.entity.Room;
import com.sisibibi.api.domain.room.entity.RoomStatus;
import com.sisibibi.api.domain.room.repository.RoomRepository;
import com.sisibibi.api.domain.roomparticipant.entity.RoomParticipantStatus;
import com.sisibibi.api.domain.roomparticipant.repository.RoomParticipantRepository;
import com.sisibibi.api.domain.roomranking.dto.response.RoomRankingRes;
import com.sisibibi.api.domain.roomranking.repository.RedisRoomRankingRepository;
import com.sisibibi.api.domain.roomranking.repository.RedisRoomRankingRepository.RoomRankingEntry;
import com.sisibibi.api.domain.speechreaction.repository.SpeechReactionRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class RoomRankingServiceTest {

  @Mock
  private RedisRoomRankingRepository redisRoomRankingRepository;

  @Mock
  private RoomRepository roomRepository;

  @Mock
  private RoomParticipantRepository roomParticipantRepository;

  @Mock
  private ChatMessageRepository chatMessageRepository;

  @Mock
  private SpeechReactionRepository speechReactionRepository;

  @InjectMocks
  private RoomRankingService roomRankingService;

  @Test
  void getRankedOpenRooms_returnsRedisRankedOpenRooms() {
    Room room = openRoom(10L, "인기 토론방");

    given(redisRoomRankingRepository.findTopRooms(10))
        .willReturn(List.of(new RoomRankingEntry(10L, 1.23)));
    given(roomRepository.findAllById(List.of(10L)))
        .willReturn(List.of(room));

    List<RoomRankingRes> result = roomRankingService.getRankedOpenRooms();

    assertThat(result).hasSize(1);
    assertThat(result.get(0).score()).isEqualTo(1.23);
    assertThat(result.get(0).room().roomId()).isEqualTo(10L);
    assertThat(result.get(0).room().title()).isEqualTo("인기 토론방");

    verify(redisRoomRankingRepository).findTopRooms(10);
    verify(roomRepository).findAllById(List.of(10L));
  }

  @Test
  void getRankedOpenRooms_fallbacksToDbOrdering_whenRedisRankingIsEmpty() {
    Room room = openRoom(10L, "DB 기준 토론방");

    given(redisRoomRankingRepository.findTopRooms(10)).willReturn(List.of());
    given(roomRepository.findByStatusOrderByCreatedAtDesc(RoomStatus.OPEN))
        .willReturn(List.of(room));

    List<RoomRankingRes> result = roomRankingService.getRankedOpenRooms();

    assertThat(result).hasSize(1);
    assertThat(result.get(0).score()).isNull();
    assertThat(result.get(0).room().title()).isEqualTo("DB 기준 토론방");

    verify(roomRepository).findByStatusOrderByCreatedAtDesc(RoomStatus.OPEN);
  }

  @Test
  void increaseChatMessageCount_refreshesRoomRankingFromDb_whenRedisUpdateFails() {
    willThrow(new RuntimeException("redis down"))
        .given(redisRoomRankingRepository)
        .increaseChatMessageCount(10L);

    given(roomRepository.existsById(10L)).willReturn(true);
    given(roomParticipantRepository.countByRoomIdAndStatus(10L, RoomParticipantStatus.JOINED))
        .willReturn(5);
    given(chatMessageRepository.countByRoomIdAndDeletedFalse(10L)).willReturn(20L);
    given(speechReactionRepository.countByRoomId(10L)).willReturn(7L);

    roomRankingService.increaseChatMessageCount(10L);

    verify(redisRoomRankingRepository).increaseChatMessageCount(10L);
    verify(redisRoomRankingRepository).setStatsAndRefreshScore(10L, 5L, 20L, 7L);
  }

  @Test
  void refreshReactionCount_setsCurrentReactionCount() {
    given(speechReactionRepository.countByRoomId(10L)).willReturn(7L);

    roomRankingService.refreshReactionCount(10L);

    verify(speechReactionRepository).countByRoomId(10L);
    verify(redisRoomRankingRepository).setReactionCount(10L, 7L);
  }

  @Test
  void removeRoom_delegatesToRedisRepository() {
    roomRankingService.removeRoom(10L);

    verify(redisRoomRankingRepository).removeRoom(10L);
  }

  private Room openRoom(Long roomId, String title) {
    Room room = Room.open(
        1L,
        title,
        LocalDateTime.of(2026, 6, 15, 10, 0),
        LocalDateTime.of(2026, 6, 15, 11, 0),
        100
    );
    ReflectionTestUtils.setField(room, "id", roomId);
    return room;
  }
}