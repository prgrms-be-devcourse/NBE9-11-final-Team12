package com.sisibibi.api.domain.chat.service;

import io.micrometer.core.instrument.MeterRegistry;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ChatPerformanceMetrics {

    static final String MESSAGE_SAVE_TIMER = "sisibibi.chat.message.save";
    static final String WEBSOCKET_PUBLISH_TIMER = "sisibibi.chat.websocket.publish";

    private final MeterRegistry meterRegistry;

    public <T> T recordMessageSave(Supplier<T> supplier) {
        return meterRegistry.timer(MESSAGE_SAVE_TIMER)
                .record(supplier);
    }

    public void recordWebSocketPublish(Runnable runnable) {
        meterRegistry.timer(WEBSOCKET_PUBLISH_TIMER)
                .record(runnable);
    }
}
