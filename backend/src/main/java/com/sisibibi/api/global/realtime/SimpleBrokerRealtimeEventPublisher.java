package com.sisibibi.api.global.realtime;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SimpleBrokerRealtimeEventPublisher implements RealtimeEventPublisher {

    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public void publish(String destination, Object event) {
        messagingTemplate.convertAndSend(destination, event);
    }
}
