package com.sisibibi.api.domain.speech.report.repository;

import com.sisibibi.api.domain.speech.report.entity.SpeechReport;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpeechReportRepository extends JpaRepository<SpeechReport, Long> {

    boolean existsBySpeechIdAndReporterUserId(Long speechId, Long reporterUserId);
}
