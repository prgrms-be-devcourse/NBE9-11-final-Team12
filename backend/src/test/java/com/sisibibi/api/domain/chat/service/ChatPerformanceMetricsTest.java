package com.sisibibi.api.domain.chat.service;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ChatPerformanceMetricsTest {

    private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
    private final ChatPerformanceMetrics metrics = new ChatPerformanceMetrics(meterRegistry);

    @Test
    void recordMessageSave_recordsTimerAndReturnsSupplierResult() {
        String result = metrics.recordMessageSave(() -> "saved");

        assertThat(result).isEqualTo("saved");
        assertThat(meterRegistry.timer(ChatPerformanceMetrics.MESSAGE_SAVE_TIMER).count())
                .isEqualTo(1);
    }

    @Test
    void recordWebSocketPublish_recordsTimer() {
        metrics.recordWebSocketPublish(() -> {
        });

        assertThat(meterRegistry.timer(ChatPerformanceMetrics.WEBSOCKET_PUBLISH_TIMER).count())
                .isEqualTo(1);
    }
}
