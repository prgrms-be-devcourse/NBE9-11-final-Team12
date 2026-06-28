package com.sisibibi.api.global.websocket;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RoomPresenceCandidateTest {

    @Test
    void member_returnsRedisZSetMember() {
        RoomPresenceCandidate candidate = new RoomPresenceCandidate(1L, 2L, 3L);

        assertThat(candidate.member()).isEqualTo("1:2:3");
    }

    @Test
    void parse_createsCandidateFromRedisZSetMember() {
        RoomPresenceCandidate candidate = RoomPresenceCandidate.parse("1:2:3");

        assertThat(candidate.roomId()).isEqualTo(1L);
        assertThat(candidate.userId()).isEqualTo(2L);
        assertThat(candidate.generation()).isEqualTo(3L);
    }

    @Test
    void parse_throwsExceptionWhenMemberIsInvalid() {
        assertThatThrownBy(() -> RoomPresenceCandidate.parse("1:2"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
