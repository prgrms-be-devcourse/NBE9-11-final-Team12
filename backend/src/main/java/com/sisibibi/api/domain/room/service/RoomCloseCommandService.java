package com.sisibibi.api.domain.room.service;

import com.sisibibi.api.domain.room.dto.event.RoomClosedEvent;
import com.sisibibi.api.domain.room.entity.Room;
import com.sisibibi.api.domain.room.entity.RoomStatus;
import com.sisibibi.api.domain.room.repository.RoomRepository;
import com.sisibibi.api.domain.speech.service.SpeakingQueueService;
import com.sisibibi.api.global.exception.CustomException;
import com.sisibibi.api.global.exception.ErrorCode;
import java.time.LocalDateTime;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;

@Service
@RequiredArgsConstructor
public class RoomCloseCommandService {

  private final RoomRepository roomRepository;
  private final ApplicationEventPublisher eventPublisher;
  private final SpeakingQueueService speakingQueueService;

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public boolean closeRoom(Long roomId, LocalDateTime now) {
    Room room = roomRepository.findByIdForUpdate(roomId)
        .orElseThrow(() -> new CustomException(ErrorCode.ROOM_NOT_FOUND));

    if (room.getStatus() == RoomStatus.CLOSED) {
      return false;
    }

    room.close(now);
    closeSpeakingQueuesAndPublishEvent(room.getId(), room.getEndedAt());
    return true;
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public boolean closeExpiredRoom(Long roomId, LocalDateTime now) {
    int updatedCount = roomRepository.closeExpiredRoomIfOpen(
        roomId,
        RoomStatus.OPEN,
        RoomStatus.CLOSED,
        now,
        now
    );

    if (updatedCount == 0) {
      return false;
    }

    closeSpeakingQueuesAndPublishEvent(roomId, now);
    return true;
  }

  private void closeSpeakingQueuesAndPublishEvent(Long roomId, LocalDateTime closedAt) {
    speakingQueueService.closeSpeakingQueuesWhenRoomClosed(roomId, closedAt);
    eventPublisher.publishEvent(new RoomClosedEvent(roomId, closedAt));
  }
}
