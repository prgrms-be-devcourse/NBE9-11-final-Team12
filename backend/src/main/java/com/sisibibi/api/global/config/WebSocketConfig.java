package com.sisibibi.api.global.config;

import com.sisibibi.api.global.websocket.session.RegistryWebSocketHandlerDecoratorFactory;
import com.sisibibi.api.global.websocket.auth.WebSocketAuthChannelInterceptor;
import com.sisibibi.api.global.websocket.auth.WebSocketAuthHandshakeInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;
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
    public static final int DEFAULT_CHANNEL_CORE_POOL_SIZE = 4;
    public static final int DEFAULT_CHANNEL_MAX_POOL_SIZE = 16;
    public static final int DEFAULT_CHANNEL_QUEUE_CAPACITY = 2_000;
    public static final int DEFAULT_HEARTBEAT_POOL_SIZE = 4;

    private final WebSocketAuthHandshakeInterceptor handshakeInterceptor;
    private final WebSocketAuthChannelInterceptor authChannelInterceptor;
    private final RegistryWebSocketHandlerDecoratorFactory webSocketHandlerDecoratorFactory;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    @Value("${app.websocket.channel.inbound.core-pool-size:" + DEFAULT_CHANNEL_CORE_POOL_SIZE + "}")
    private int inboundCorePoolSize;

    @Value("${app.websocket.channel.inbound.max-pool-size:" + DEFAULT_CHANNEL_MAX_POOL_SIZE + "}")
    private int inboundMaxPoolSize;

    @Value("${app.websocket.channel.inbound.queue-capacity:" + DEFAULT_CHANNEL_QUEUE_CAPACITY + "}")
    private int inboundQueueCapacity;

    @Value("${app.websocket.channel.outbound.core-pool-size:" + DEFAULT_CHANNEL_CORE_POOL_SIZE + "}")
    private int outboundCorePoolSize;

    @Value("${app.websocket.channel.outbound.max-pool-size:" + DEFAULT_CHANNEL_MAX_POOL_SIZE + "}")
    private int outboundMaxPoolSize;

    @Value("${app.websocket.channel.outbound.queue-capacity:" + DEFAULT_CHANNEL_QUEUE_CAPACITY + "}")
    private int outboundQueueCapacity;

    @Value("${app.websocket.heartbeat.pool-size:" + DEFAULT_HEARTBEAT_POOL_SIZE + "}")
    private int heartbeatPoolSize;

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
        registration.taskExecutor()
                .corePoolSize(inboundCorePoolSize)
                .maxPoolSize(inboundMaxPoolSize)
                .queueCapacity(inboundQueueCapacity);
        registration.interceptors(authChannelInterceptor);
    }

    @Override
    public void configureClientOutboundChannel(ChannelRegistration registration) {
        registration.taskExecutor()
                .corePoolSize(outboundCorePoolSize)
                .maxPoolSize(outboundMaxPoolSize)
                .queueCapacity(outboundQueueCapacity);
    }

    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registration) {
        registration.addDecoratorFactory(webSocketHandlerDecoratorFactory);
    }

    @Bean
    public ThreadPoolTaskScheduler webSocketHeartbeatTaskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(heartbeatPoolSize);
        scheduler.setThreadNamePrefix("websocket-heartbeat-");
        return scheduler;
    }
}
