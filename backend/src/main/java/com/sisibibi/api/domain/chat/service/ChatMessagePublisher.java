package com.sisibibi.api.domain.chat.service;

import com.sisibibi.api.domain.chat.dto.response.ChatEventRes;

public interface ChatMessagePublisher {

    void publish(ChatEventRes event);
}
