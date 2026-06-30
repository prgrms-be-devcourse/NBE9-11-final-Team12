package com.sisibibi.api.global.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WebSocketConfigTest {

    @Test
    void websocketConstants_followStompMvpContract() {
        assertThat(WebSocketConfig.ENDPOINT).isEqualTo("/api/v1/ws");
        assertThat(WebSocketConfig.APPLICATION_DESTINATION_PREFIX).isEqualTo("/app");
        assertThat(WebSocketConfig.BROKER_DESTINATION_PREFIXES).containsExactly("/topic", "/queue");
        assertThat(WebSocketConfig.USER_DESTINATION_PREFIX).isEqualTo("/user");
        assertThat(WebSocketConfig.HEARTBEAT_MILLIS).isEqualTo(10_000L);
        assertThat(WebSocketConfig.DEFAULT_CHANNEL_CORE_POOL_SIZE).isEqualTo(4);
        assertThat(WebSocketConfig.DEFAULT_CHANNEL_MAX_POOL_SIZE).isEqualTo(16);
        assertThat(WebSocketConfig.DEFAULT_CHANNEL_QUEUE_CAPACITY).isEqualTo(2_000);
        assertThat(WebSocketConfig.DEFAULT_HEARTBEAT_POOL_SIZE).isEqualTo(4);
    }
}
