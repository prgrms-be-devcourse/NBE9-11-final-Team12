package com.sisibibi.api.domain.room.scheduler;

import com.sisibibi.api.domain.room.service.RoomService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class RoomCloseScheduler {

  private final RoomService roomService;

  @Scheduled(fixedDelayString = "${app.room.close-fixed-delay-ms:60000}")
  public void closeExpiredRooms() {
   roomService.closeExpiredRooms(LocalDateTime.now());
  }
}