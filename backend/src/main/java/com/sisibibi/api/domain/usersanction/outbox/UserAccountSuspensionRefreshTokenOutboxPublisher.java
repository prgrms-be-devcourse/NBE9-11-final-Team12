package com.sisibibi.api.domain.usersanction.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sisibibi.api.global.outbox.OutboxEvent;
import com.sisibibi.api.global.outbox.OutboxEventPublisher;
import com.sisibibi.api.global.outbox.OutboxEventType;
import com.sisibibi.api.global.security.refresh.RefreshTokenStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserAccountSuspensionRefreshTokenOutboxPublisher implements OutboxEventPublisher {

  private final ObjectMapper objectMapper;
  private final RefreshTokenStore refreshTokenStore;

  @Override
  public boolean supports(OutboxEventType eventType) {
    return eventType == OutboxEventType.ACCOUNT_SUSPENSION_REFRESH_TOKEN_DELETE_REQUESTED;
  }

  //삭제된 토큰이 없어도 다시 삭제해도 되는 멱등 작업
  @Override
  public void publish(OutboxEvent event) {
    refreshTokenStore.deleteAll(userIdFrom(event));
  }

  private Long userIdFrom(OutboxEvent event) {
    try {
      JsonNode payload = objectMapper.readTree(event.getPayload());
      return payload.get("userId").asLong();
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException(
          "Failed to deserialize account suspension refresh token deletion request.",
          exception
      );
    }
  }
}