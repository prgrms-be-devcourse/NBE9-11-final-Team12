package com.sisibibi.api.domain.speechreport.repository;

import com.sisibibi.api.domain.speechreport.entity.SpeechReport;
import com.sisibibi.api.domain.speechreport.entity.SpeechReportReason;
import com.sisibibi.api.domain.speechreport.entity.SpeechReportStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.Optional;

public interface SpeechReportRepository extends JpaRepository<SpeechReport, Long> {

    boolean existsBySpeechIdAndReporterUserId(Long speechId, Long reporterUserId);

    @Query("""
            select report
            from SpeechReport report
            where (:status is null or report.status = :status)
              and (:reason is null or report.reason = :reason)
            """)
    Page<SpeechReport> findAllByFilters(
            SpeechReportStatus status,
            SpeechReportReason reason,
            Pageable pageable
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select report from SpeechReport report where report.id = :reportId")
    Optional<SpeechReport> findByIdForUpdate(@Param("reportId") Long reportId);

    @Query("""
            select
                sum(case when report.severity = com.sisibibi.api.domain.speechreport.entity.ViolationSeverity.LOW then 1 else 0 end) as lowCount,
                sum(case when report.severity = com.sisibibi.api.domain.speechreport.entity.ViolationSeverity.MEDIUM then 1 else 0 end) as mediumCount,
                sum(case when report.severity = com.sisibibi.api.domain.speechreport.entity.ViolationSeverity.HIGH then 1 else 0 end) as highCount,
                sum(case when report.severity = com.sisibibi.api.domain.speechreport.entity.ViolationSeverity.CRITICAL then 1 else 0 end) as criticalCount
            from SpeechReport report
            where report.reportedUserId = :userId
              and report.status = com.sisibibi.api.domain.speechreport.entity.SpeechReportStatus.RESOLVED
              and report.reviewedAt >= :reviewedSince
            """)
    ViolationHistorySummaryProjection summarizeResolvedViolations(
            @Param("userId") Long userId,
            @Param("reviewedSince") LocalDateTime reviewedSince
    );
}
