package com.sisibibi.api.domain.report.repository;

import com.sisibibi.api.domain.report.entity.AiReport;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AiReportRepository extends JpaRepository<AiReport, Long> {

    Optional<AiReport> findByRoomId(Long roomId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select report from AiReport report where report.roomId = :roomId")
    Optional<AiReport> findByRoomIdForUpdate(@Param("roomId") Long roomId);
}
