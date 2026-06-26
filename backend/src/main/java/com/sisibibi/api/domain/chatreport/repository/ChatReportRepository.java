package com.sisibibi.api.domain.chatreport.repository;

import com.sisibibi.api.domain.chatreport.entity.ChatReport;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatReportRepository extends JpaRepository<ChatReport, Long> {

    boolean existsByMessageIdAndReporterUserId(Long messageId, Long reporterUserId);
}
