package com.sisibibi.api.domain.chatreport.repository;

import com.sisibibi.api.domain.chatreport.entity.ChatReport;
import com.sisibibi.api.domain.chatreport.entity.ChatReportReason;
import com.sisibibi.api.domain.chatreport.entity.ChatReportStatus;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChatReportRepository extends JpaRepository<ChatReport, Long> {

    boolean existsByMessageIdAndReporterUserId(Long messageId, Long reporterUserId);

    @Query("""
            select report
            from ChatReport report
            where (:status is null or report.status = :status)
              and (:reason is null or report.reason = :reason)
            """)
    Page<ChatReport> findAllByFilters(
            ChatReportStatus status,
            ChatReportReason reason,
            Pageable pageable
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select report from ChatReport report where report.id = :reportId")
    Optional<ChatReport> findByIdForUpdate(@Param("reportId") Long reportId);
}
