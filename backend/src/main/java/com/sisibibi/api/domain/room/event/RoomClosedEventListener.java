package com.sisibibi.api.domain.room.event;

import com.sisibibi.api.domain.room.dto.event.RoomClosedEvent;
import com.sisibibi.api.domain.room.dto.event.RoomClosedMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class RoomClosedEventListener {

  private final SimpMessagingTemplate messagingTemplate;

  @Async
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handle(RoomClosedEvent event) {
    messagingTemplate.convertAndSend(
        "/topic/rooms/" + event.roomId() + "/events",
        RoomClosedMessage.of(event.roomId())
    );
  }
}