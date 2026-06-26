package com.sisibibi.api.domain.speech.repository;

import com.sisibibi.api.domain.speech.entity.StageSummary;
import com.sisibibi.api.domain.speech.entity.StageSummaryStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StageSummaryRepository extends JpaRepository<StageSummary, Long> {

    Optional<StageSummary> findByRoomId(Long roomId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select summary from StageSummary summary where summary.roomId = :roomId")
    Optional<StageSummary> findByRoomIdForUpdate(@Param("roomId") Long roomId);

    @Query("""
            select summary
            from StageSummary summary
            where summary.status = :status
              and summary.retryCount < :maxAttempts
            order by summary.createdAt asc, summary.id asc
            """)
    List<StageSummary> findRetryCandidates(
            @Param("status") StageSummaryStatus status,
            @Param("maxAttempts") int maxAttempts,
            Pageable pageable
    );
}
