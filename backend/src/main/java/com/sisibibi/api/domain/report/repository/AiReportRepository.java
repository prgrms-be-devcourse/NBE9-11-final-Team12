package com.sisibibi.api.domain.report.repository;

import com.sisibibi.api.domain.report.entity.AiReport;
import com.sisibibi.api.domain.report.entity.AiReportStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AiReportRepository extends JpaRepository<AiReport, Long> {

    Optional<AiReport> findByRoomId(Long roomId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select report from AiReport report where report.roomId = :roomId")
    Optional<AiReport> findByRoomIdForUpdate(@Param("roomId") Long roomId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select report from AiReport report where report.id = :reportId")
    Optional<AiReport> findByIdForUpdate(@Param("reportId") Long reportId);

    @Query("""
            select report
            from AiReport report
            where report.status in :statuses
              and report.updatedAt <= :updatedBefore
              and report.publishRetryCount < :maxRetryCount
            order by report.updatedAt asc, report.id asc
            """)
    List<AiReport> findPublishRetryCandidates(
            @Param("statuses") List<AiReportStatus> statuses,
            @Param("updatedBefore") LocalDateTime updatedBefore,
            @Param("maxRetryCount") int maxRetryCount,
            Pageable pageable
    );
}
