package com.sisibibi.api.domain.room.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class RoomTest {

  @Test
  void close_preservesFirstClosedAt_whenAlreadyClosed() {
    LocalDateTime startedAt = LocalDateTime.of(2026, 6, 18, 10, 0);
    LocalDateTime scheduledEndAt = startedAt.plusHours(2);
    LocalDateTime firstClosedAt = startedAt.plusMinutes(30);
    LocalDateTime secondClosedAt = startedAt.plusMinutes(40);
    Room room = Room.open(1L, "토론방", startedAt, scheduledEndAt, 100);

    room.close(firstClosedAt);
    room.close(secondClosedAt);

    assertThat(room.getStatus()).isEqualTo(RoomStatus.CLOSED);
    assertThat(room.getEndedAt()).isEqualTo(firstClosedAt);
  }

  @Test
  void update_preservesExistingValues_whenAllInputsAreNull() {
    LocalDateTime startedAt = LocalDateTime.of(2026, 6, 18, 10, 0);
    LocalDateTime endedAt = startedAt.plusHours(2);
    Room room = Room.open(1L, "기존 제목", startedAt, endedAt, 100);

    room.update(null, null, null, null);

    assertThat(room.getTitle()).isEqualTo("기존 제목");
    assertThat(room.getStartedAt()).isEqualTo(startedAt);
    assertThat(room.getEndedAt()).isEqualTo(endedAt);
  }

  @Test
  void isActiveAt_returnsTrue_whenRoomIsOpenAndEndTimeIsFuture() {
    LocalDateTime now = LocalDateTime.of(2026, 6, 18, 10, 0);
    Room room = Room.open(1L, "토론방", now.minusMinutes(10), now.plusMinutes(10), 100);

    assertThat(room.isActiveAt(now)).isTrue();
    assertThat(room.isJoinableAt(now)).isTrue();
  }

  @Test
  void isActiveAt_returnsFalse_whenRoomEndTimeHasPassed() {
    LocalDateTime now = LocalDateTime.of(2026, 6, 18, 10, 0);
    Room room = Room.open(1L, "토론방", now.minusMinutes(20), now.minusMinutes(1), 100);

    assertThat(room.isActiveAt(now)).isFalse();
    assertThat(room.isJoinableAt(now)).isFalse();
  }
}
