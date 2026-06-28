package com.sisibibi.api.domain.usersanction.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sisibibi.api.global.outbox.OutboxEvent;
import com.sisibibi.api.global.outbox.OutboxEventRepository;
import com.sisibibi.api.global.outbox.OutboxEventType;
import java.time.LocalDateTime;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserAccountSuspensionRefreshTokenOutboxWriter {

  private static final String AGGREGATE_TYPE = "USER_SANCTION";
  private static final String DEDUPLICATION_KEY_PREFIX =
      "ACCOUNT_SUSPENSION_REFRESH_TOKEN_DELETE_REQUESTED:";

  private final OutboxEventRepository outboxEventRepository;
  private final ObjectMapper objectMapper;

  @Transactional(propagation = Propagation.MANDATORY)
  public void record(Long sanctionId, Long userId, LocalDateTime occurredAt) {
    String deduplicationKey = DEDUPLICATION_KEY_PREFIX + sanctionId;

    if (outboxEventRepository.existsByDeduplicationKey(deduplicationKey)) {
      return;
    }

    outboxEventRepository.save(OutboxEvent.pending(
        AGGREGATE_TYPE,
        sanctionId,
        OutboxEventType.ACCOUNT_SUSPENSION_REFRESH_TOKEN_DELETE_REQUESTED,
        payload(sanctionId, userId, occurredAt),
        deduplicationKey,
        occurredAt
    ));
  }

  private String payload(Long sanctionId, Long userId, LocalDateTime occurredAt) {
    try {
      return objectMapper.writeValueAsString(Map.of(
          "sanctionId", sanctionId,
          "userId", userId,
          "occurredAt", occurredAt.toString()
      ));
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException(
          "Failed to serialize account suspension refresh token deletion request.",
          exception
      );
    }
  }
}