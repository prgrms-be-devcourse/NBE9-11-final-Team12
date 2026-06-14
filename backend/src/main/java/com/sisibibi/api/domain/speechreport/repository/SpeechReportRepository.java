package com.sisibibi.api.domain.speechreport.repository;

import com.sisibibi.api.domain.speechreport.entity.SpeechReport;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpeechReportRepository extends JpaRepository<SpeechReport, Long> {

    boolean existsBySpeechIdAndReporterUserId(Long speechId, Long reporterUserId);
}
