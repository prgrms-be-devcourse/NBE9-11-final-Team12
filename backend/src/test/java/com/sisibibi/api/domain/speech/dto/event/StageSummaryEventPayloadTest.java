package com.sisibibi.api.domain.speech.dto.event;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class StageSummaryEventPayloadTest {

    @Test
    void payloadContainsOnlyIdentifiersSoClientsReloadSummaryFromApi() {
        assertThat(StageSummaryEventPayload.class.getRecordComponents())
                .extracting(component -> component.getName())
                .containsExactly("summaryId", "roomId");
    }
}
