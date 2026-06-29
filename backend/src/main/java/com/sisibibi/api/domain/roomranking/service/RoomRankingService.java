package com.sisibibi.api.domain.roomranking.service;

import com.sisibibi.api.domain.chat.repository.ChatMessageRepository;
import com.sisibibi.api.domain.roomranking.dto.response.RoomRankingRes;
import com.sisibibi.api.domain.room.dto.response.RoomSummaryRes;
import com.sisibibi.api.domain.room.entity.Room;
import com.sisibibi.api.domain.room.entity.RoomStatus;
import com.sisibibi.api.domain.roomranking.repository.RedisRoomRankingRepository;
import com.sisibibi.api.domain.roomranking.repository.RedisRoomRankingRepository.RoomRankingEntry;
import com.sisibibi.api.domain.room.repository.RoomRepository;
import com.sisibibi.api.domain.roomparticipant.entity.RoomParticipantStatus;
import com.sisibibi.api.domain.roomparticipant.repository.RoomParticipantRepository;
import com.sisibibi.api.domain.speechreaction.repository.SpeechReactionRepository;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoomRankingService {

  private static final int DEFAULT_RANKING_LIMIT = 10;

  private final RedisRoomRankingRepository redisRoomRankingRepository;
  private final RoomRepository roomRepository;
  private final RoomParticipantRepository roomParticipantRepository;
  private final ChatMessageRepository chatMessageRepository;
  private final SpeechReactionRepository speechReactionRepository;

  public List<RoomRankingRes> getRankedOpenRooms() {
    return getRankedOpenRooms(DEFAULT_RANKING_LIMIT);
  }

  public List<RoomRankingRes> getRankedOpenRooms(int limit) {
    try {
      List<RoomRankingEntry> entries = redisRoomRankingRepository.findTopRooms(limit);
      if (entries.isEmpty()) {
        return fallbackOpenRooms(limit);
      }

      Map<Long, Room> roomsById = roomRepository.findAllById(
              entries.stream().map(RoomRankingEntry::roomId).toList()
          )
          .stream()
          .filter(room -> room.getStatus() == RoomStatus.OPEN)
          .collect(Collectors.toMap(Room::getId, room -> room));

      return entries.stream()
          .filter(entry -> roomsById.containsKey(entry.roomId()))
          .map(entry -> new RoomRankingRes(
              null,
              entry.score(),
              RoomSummaryRes.from(roomsById.get(entry.roomId()))
          ))
          .toList();
    } catch (RuntimeException exception) {
      log.warn("Redis room ranking read failed. fallback to DB ordering.", exception);
      return fallbackOpenRooms(limit);
    }
  }

  public void setParticipantCount(Long roomId, long participantCount) {
    try {
      redisRoomRankingRepository.setParticipantCount(roomId, participantCount);
    } catch (RuntimeException exception) {
      log.warn("Redis participant ranking update failed. roomId={}", roomId, exception);
      refreshRoomRankingFromDb(roomId);
    }
  }

  public void increaseChatMessageCount(Long roomId) {
    try {
      redisRoomRankingRepository.increaseChatMessageCount(roomId);
    } catch (RuntimeException exception) {
      log.warn("Redis chat ranking update failed. roomId={}", roomId, exception);
      refreshRoomRankingFromDb(roomId);
    }
  }

  public void decreaseChatMessageCount(Long roomId) {
    try {
      redisRoomRankingRepository.decreaseChatMessageCount(roomId);
    } catch (RuntimeException exception) {
      log.warn("Redis chat ranking rollback failed. roomId={}", roomId, exception);
      refreshRoomRankingFromDb(roomId);
    }
  }

  public void refreshReactionCount(Long roomId) {
    try {
      long reactionCount = speechReactionRepository.countByRoomId(roomId);
      redisRoomRankingRepository.setReactionCount(roomId, reactionCount);
    } catch (RuntimeException exception) {
      log.warn("Redis reaction ranking update failed. roomId={}", roomId, exception);
      refreshRoomRankingFromDb(roomId);
    }
  }

  public void removeRoom(Long roomId) {
    try {
      redisRoomRankingRepository.removeRoom(roomId);
    } catch (RuntimeException exception) {
      log.warn("Redis room ranking remove failed. roomId={}", roomId, exception);
    }
  }

  private void refreshRoomRankingFromDb(Long roomId) {
    try {
      if (!roomRepository.existsById(roomId)) {
        redisRoomRankingRepository.removeRoom(roomId);
        return;
      }

      long participantCount = roomParticipantRepository.countByRoomIdAndStatus(
          roomId,
          RoomParticipantStatus.JOINED
      );
      long chatMessageCount = chatMessageRepository.countByRoomIdAndDeletedFalse(roomId);
      long reactionCount = speechReactionRepository.countByRoomId(roomId);

      redisRoomRankingRepository.setStatsAndRefreshScore(
          roomId,
          participantCount,
          chatMessageCount,
          reactionCount
      );
    } catch (RuntimeException exception) {
      log.warn("Redis room ranking DB refresh failed. roomId={}", roomId, exception);
    }
  }

  private List<RoomRankingRes> fallbackOpenRooms(int limit) {
    return roomRepository.findByStatusOrderByCreatedAtDesc(RoomStatus.OPEN)
        .stream()
        .limit(limit)
        .map(room -> new RoomRankingRes(null, null, RoomSummaryRes.from(room)))
        .toList();
  }
}