package com.sisibibi.api.global.outbox;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

  boolean existsByDeduplicationKey(String deduplicationKey);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("""
            select event
            from OutboxEvent event
            where event.status = :status
              and event.nextRetryAt <= :now
            order by event.occurredAt asc, event.id asc
            """)
  List<OutboxEvent> findReadyEventsForUpdate(
      @Param("status") OutboxEventStatus status,
      @Param("now") LocalDateTime now,
      Pageable pageable
  );

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("""
        select event
        from OutboxEvent event
        where event.id = :eventId
        """)
  OutboxEvent findByIdForUpdate(@Param("eventId") Long eventId);
}