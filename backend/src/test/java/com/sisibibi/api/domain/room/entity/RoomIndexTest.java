package com.sisibibi.api.domain.room.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import jakarta.persistence.Index;
import jakarta.persistence.Table;
import org.junit.jupiter.api.Test;

class RoomIndexTest {

    @Test
    void roomDefinesStageSummaryCandidateLookupIndex() {
        Table table = Room.class.getAnnotation(Table.class);

        assertThat(table.indexes())
                .extracting(Index::name, Index::columnList)
                .contains(tuple(
                        "idx_rooms_status_started_ended_id",
                        "status, started_at, ended_at, id"
                ));
    }
}
