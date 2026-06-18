package com.sisibibi.api.domain.chat.service;

import com.sisibibi.api.domain.chat.dto.response.ChatEventRes;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StompChatMessagePublisher implements ChatMessagePublisher {

    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public void publish(ChatEventRes event) {
        messagingTemplate.convertAndSend(
                "/topic/rooms/" + event.roomId() + "/chat/messages",
                event
        );
    }
}
