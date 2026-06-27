package com.sisibibi.api.global.outbox;

public enum OutboxEventStatus {
  PENDING,
  PUBLISHING,
  SENT,
  FAILED
}