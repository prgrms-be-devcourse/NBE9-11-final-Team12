package com.sisibibi.api.domain.chat.controller;

import com.sisibibi.api.domain.chat.dto.request.ChatMessageReq;
import com.sisibibi.api.domain.chat.service.ChatService;
import com.sisibibi.api.global.exception.CustomException;
import com.sisibibi.api.global.exception.ErrorCode;
import com.sisibibi.api.global.security.AuthPrincipal;
import jakarta.validation.Valid;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;

@Validated
@Controller
@RequiredArgsConstructor
public class ChatMessageController {

    private final ChatService chatService;

    @MessageMapping("/rooms/{roomId}/chat/messages")
    public void createMessage(
            @DestinationVariable Long roomId,
            @Valid ChatMessageReq request,
            Principal principal
    ) {
        AuthPrincipal authPrincipal = resolvePrincipal(principal);
        chatService.createMessage(roomId, authPrincipal.userId(), request.content());
    }

    private AuthPrincipal resolvePrincipal(Principal principal) {
        if (principal instanceof Authentication authentication
                && authentication.getPrincipal() instanceof AuthPrincipal authPrincipal) {
            return authPrincipal;
        }

        throw new CustomException(ErrorCode.UNAUTHORIZED);
    }
}
