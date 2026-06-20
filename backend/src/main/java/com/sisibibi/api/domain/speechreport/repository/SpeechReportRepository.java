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
}
