package com.sisibibi.api.domain.roomranking.event;

import static org.mockito.Mockito.verify;

import com.sisibibi.api.domain.chat.dto.event.ChatEventType;
import com.sisibibi.api.domain.chat.dto.event.ChatMessageChangedEvent;
import com.sisibibi.api.domain.room.dto.event.RoomClosedEvent;
import com.sisibibi.api.domain.roomparticipant.dto.event.RoomParticipantChangedEvent;
import com.sisibibi.api.domain.roomparticipant.dto.event.RoomParticipantEventPayload;
import com.sisibibi.api.domain.roomparticipant.dto.event.RoomParticipantEventType;
import com.sisibibi.api.domain.roomranking.service.RoomRankingService;
import com.sisibibi.api.domain.speechreaction.dto.event.SpeechReactionChangedEvent;
import com.sisibibi.api.domain.speechreaction.dto.event.SpeechReactionEventPayload;
import com.sisibibi.api.domain.speechreaction.dto.event.SpeechReactionEventType;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RoomRankingEventListenerTest {

  @Mock
  private RoomRankingService roomRankingService;

  @InjectMocks
  private RoomRankingEventListener roomRankingEventListener;

  @Test
  void handle_updatesParticipantCount_whenParticipantChanged() {
    RoomParticipantChangedEvent event = new RoomParticipantChangedEvent(
        RoomParticipantEventType.PARTICIPANT_JOINED,
        10L,
        RoomParticipantEventPayload.of(10L, 1L, 5)
    );

    roomRankingEventListener.handle(event);

    verify(roomRankingService).setParticipantCount(10L, 5);
  }

  @Test
  void handle_increasesChatMessageCount_whenMessageCreated() {
    ChatMessageChangedEvent event = new ChatMessageChangedEvent(
        ChatEventType.MESSAGE_CREATED,
        10L,
        null
    );

    roomRankingEventListener.handle(event);

    verify(roomRankingService).increaseChatMessageCount(10L);
  }

  @Test
  void handle_decreasesChatMessageCount_whenMessageDeleted() {
    ChatMessageChangedEvent event = new ChatMessageChangedEvent(
        ChatEventType.MESSAGE_DELETED,
        10L,
        null
    );

    roomRankingEventListener.handle(event);

    verify(roomRankingService).decreaseChatMessageCount(10L);
  }

  @Test
  void handle_refreshesReactionCount_whenReactionChanged() {
    SpeechReactionChangedEvent event = new SpeechReactionChangedEvent(
        SpeechReactionEventType.SPEECH_REACTION_CHANGED,
        10L,
        SpeechReactionEventPayload.of(10L, 100L, 3)
    );

    roomRankingEventListener.handle(event);

    verify(roomRankingService).refreshReactionCount(10L);
  }

  @Test
  void handle_removesRoomRanking_whenRoomClosed() {
    RoomClosedEvent event = new RoomClosedEvent(
        10L,
        LocalDateTime.of(2026, 6, 15, 11, 0)
    );

    roomRankingEventListener.handle(event);

    verify(roomRankingService).removeRoom(10L);
  }
}