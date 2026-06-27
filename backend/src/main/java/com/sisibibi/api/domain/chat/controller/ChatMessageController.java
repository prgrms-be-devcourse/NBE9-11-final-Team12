package com.sisibibi.api.domain.chat.controller;

import com.sisibibi.api.domain.chat.dto.request.ChatMessageReq;
import com.sisibibi.api.domain.chat.dto.response.ChatMessageCursorPageRes;
import com.sisibibi.api.domain.chat.service.ChatService;
import com.sisibibi.api.global.exception.CustomException;
import com.sisibibi.api.global.exception.ErrorCode;
import com.sisibibi.api.global.response.ApiResponse;
import com.sisibibi.api.global.security.AuthPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Tag(name = "채팅", description = "채팅 메시지 조회, 삭제 API")
@Validated
@Controller
@RequiredArgsConstructor
@RequestMapping("/api/v1")
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

    @Operation(
        summary = "채팅 메시지 목록 조회",
        description = "지정한 토론방의 채팅 메시지를 커서 기반으로 조회합니다."
    )
    @ResponseBody
    @GetMapping("/rooms/{roomId}/chat/messages")
    public ResponseEntity<ApiResponse<ChatMessageCursorPageRes>> getMessages(
            @PathVariable @Positive Long roomId,
            @AuthenticationPrincipal AuthPrincipal principal,
            @RequestParam(required = false) @Positive Long cursor,
            @RequestParam(defaultValue = "50") @Min(1) @Max(50) int limit
    ) {
        AuthPrincipal authPrincipal = requirePrincipal(principal);

        return ResponseEntity.ok(ApiResponse.ok(
                "채팅 메시지 목록 조회가 완료되었습니다.",
                chatService.getMessages(roomId, authPrincipal.userId(), cursor, limit)
        ));
    }

    @Operation(
        summary = "채팅 메시지 삭제",
        description = "현재 로그인 사용자가 작성한 채팅 메시지를 삭제합니다."
    )
    @ResponseBody
    @DeleteMapping("/rooms/{roomId}/chat/messages/{messageId}")
    public ResponseEntity<ApiResponse<Void>> deleteMessage(
            @PathVariable @Positive Long roomId,
            @PathVariable @Positive Long messageId,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        AuthPrincipal authPrincipal = requirePrincipal(principal);
        chatService.deleteMessage(roomId, messageId, authPrincipal.userId());

        return ResponseEntity.ok(ApiResponse.okMessage("채팅 메시지 삭제가 완료되었습니다."));
    }

    private AuthPrincipal resolvePrincipal(Principal principal) {
        if (principal instanceof Authentication authentication
                && authentication.getPrincipal() instanceof AuthPrincipal authPrincipal) {
            return authPrincipal;
        }

        throw new CustomException(ErrorCode.UNAUTHORIZED);
    }

    private AuthPrincipal requirePrincipal(AuthPrincipal principal) {
        if (principal == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }

        return principal;
    }
}
