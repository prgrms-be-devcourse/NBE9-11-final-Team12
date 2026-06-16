package com.sisibibi.api.global.config;

import com.sisibibi.api.global.websocket.WebSocketAuthChannelInterceptor;
import com.sisibibi.api.global.websocket.WebSocketAuthHandshakeInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@RequiredArgsConstructor
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    public static final String ENDPOINT = "/api/v1/ws";
    public static final String APPLICATION_DESTINATION_PREFIX = "/app";
    public static final String[] BROKER_DESTINATION_PREFIXES = {"/topic", "/queue"};
    public static final String USER_DESTINATION_PREFIX = "/user";
    public static final long HEARTBEAT_MILLIS = 10_000L;

    private final WebSocketAuthHandshakeInterceptor handshakeInterceptor;
    private final WebSocketAuthChannelInterceptor authChannelInterceptor;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint(ENDPOINT)
                .setAllowedOrigins(frontendUrl)
                .addInterceptors(handshakeInterceptor);
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.setApplicationDestinationPrefixes(APPLICATION_DESTINATION_PREFIX);
        registry.setUserDestinationPrefix(USER_DESTINATION_PREFIX);
        registry.enableSimpleBroker(BROKER_DESTINATION_PREFIXES)
                .setTaskScheduler(webSocketHeartbeatTaskScheduler())
                .setHeartbeatValue(new long[]{HEARTBEAT_MILLIS, HEARTBEAT_MILLIS});
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(authChannelInterceptor);
    }

    @Bean
    public ThreadPoolTaskScheduler webSocketHeartbeatTaskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("websocket-heartbeat-");
        return scheduler;
    }
}
