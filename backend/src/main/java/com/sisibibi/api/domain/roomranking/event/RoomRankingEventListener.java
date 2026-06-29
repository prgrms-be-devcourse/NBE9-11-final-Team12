package com.sisibibi.api.domain.roomranking.event;

import com.sisibibi.api.domain.chat.dto.event.ChatEventType;
import com.sisibibi.api.domain.chat.dto.event.ChatMessageChangedEvent;
import com.sisibibi.api.domain.room.dto.event.RoomClosedEvent;
import com.sisibibi.api.domain.roomranking.service.RoomRankingService;
import com.sisibibi.api.domain.roomparticipant.dto.event.RoomParticipantChangedEvent;
import com.sisibibi.api.domain.speechreaction.dto.event.SpeechReactionChangedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class RoomRankingEventListener {

  private final RoomRankingService roomRankingService;

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handle(RoomParticipantChangedEvent event) {
    roomRankingService.setParticipantCount(
        event.roomId(),
        event.payload().participantCount()
    );
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handle(ChatMessageChangedEvent event) {
    if (event.type() == ChatEventType.MESSAGE_CREATED) {
      roomRankingService.increaseChatMessageCount(event.roomId());
      return;
    }

    if (event.type() == ChatEventType.MESSAGE_DELETED) {
      roomRankingService.decreaseChatMessageCount(event.roomId());
    }
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handle(SpeechReactionChangedEvent event) {
    roomRankingService.refreshReactionCount(event.roomId());
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handle(RoomClosedEvent event) {
    roomRankingService.removeRoom(event.roomId());
  }
}