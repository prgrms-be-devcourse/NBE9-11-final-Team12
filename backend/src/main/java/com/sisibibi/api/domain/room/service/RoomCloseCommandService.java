package com.sisibibi.api.domain.room.service;

import com.sisibibi.api.domain.room.dto.event.RoomClosedEvent;
import com.sisibibi.api.domain.room.entity.RoomStatus;
import com.sisibibi.api.domain.room.repository.RoomRepository;
import com.sisibibi.api.domain.speech.service.SpeakingQueueService;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class RoomCloseCommandService {

  private final RoomRepository roomRepository;
  private final ApplicationEventPublisher eventPublisher;
  private final SpeakingQueueService speakingQueueService;

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

    speakingQueueService.closeSpeakingQueuesWhenRoomClosed(roomId, now);
    eventPublisher.publishEvent(new RoomClosedEvent(roomId, now));
    return true;
  }
}
