package com.sisibibi.api.domain.report.repository;

import com.sisibibi.api.domain.report.entity.AiReportNotificationStatus;
import com.sisibibi.api.domain.report.entity.AiReportPdfExport;
import com.sisibibi.api.domain.report.entity.AiReportPdfStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AiReportPdfExportRepository extends JpaRepository<AiReportPdfExport, Long> {

    Optional<AiReportPdfExport> findByAiReportIdAndRequestedByUserId(Long aiReportId, Long requestedByUserId);

    Optional<AiReportPdfExport> findByRoomIdAndRequestedByUserId(Long roomId, Long requestedByUserId);

    List<AiReportPdfExport> findByAiReportId(Long aiReportId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select pdfExport from AiReportPdfExport pdfExport where pdfExport.id = :id")
    Optional<AiReportPdfExport> findByIdForUpdate(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select pdfExport
            from AiReportPdfExport pdfExport
            where pdfExport.aiReportId = :aiReportId
              and pdfExport.requestedByUserId = :requestedByUserId
            """)
    Optional<AiReportPdfExport> findByAiReportIdAndRequestedByUserIdForUpdate(
            @Param("aiReportId") Long aiReportId,
            @Param("requestedByUserId") Long requestedByUserId
    );

    @Query("""
            select pdfExport
            from AiReportPdfExport pdfExport
            where pdfExport.pdfStatus = :status
              and pdfExport.pdfLastAttemptedAt <= :attemptedBefore
              and pdfExport.pdfRetryCount < :maxRetryCount
            order by pdfExport.pdfLastAttemptedAt asc, pdfExport.id asc
            """)
    List<AiReportPdfExport> findPdfRetryCandidates(
            @Param("status") AiReportPdfStatus status,
            @Param("attemptedBefore") LocalDateTime attemptedBefore,
            @Param("maxRetryCount") int maxRetryCount,
            Pageable pageable
    );

    @Query("""
            select pdfExport
            from AiReportPdfExport pdfExport
            where pdfExport.pdfStatus = :pdfStatus
              and pdfExport.notificationStatus = :notificationStatus
              and pdfExport.notificationLastAttemptedAt <= :attemptedBefore
              and pdfExport.notificationRetryCount < :maxRetryCount
            order by pdfExport.notificationLastAttemptedAt asc, pdfExport.id asc
            """)
    List<AiReportPdfExport> findNotificationRetryCandidates(
            @Param("pdfStatus") AiReportPdfStatus pdfStatus,
            @Param("notificationStatus") AiReportNotificationStatus notificationStatus,
            @Param("attemptedBefore") LocalDateTime attemptedBefore,
            @Param("maxRetryCount") int maxRetryCount,
            Pageable pageable
    );
}
