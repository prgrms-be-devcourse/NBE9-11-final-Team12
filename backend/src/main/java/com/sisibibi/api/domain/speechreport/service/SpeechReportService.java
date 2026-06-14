package com.sisibibi.api.domain.speechreport.service;

import com.sisibibi.api.domain.speech.entity.Speech;
import com.sisibibi.api.domain.speech.repository.SpeechRepository;
import com.sisibibi.api.domain.speechreport.dto.command.SpeechReportCreateCommand;
import com.sisibibi.api.domain.speechreport.dto.response.SpeechReportCreateRes;
import com.sisibibi.api.domain.speechreport.entity.SpeechReport;
import com.sisibibi.api.domain.speechreport.repository.SpeechReportRepository;
import com.sisibibi.api.global.exception.CustomException;
import com.sisibibi.api.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SpeechReportService {

    private final SpeechRepository speechRepository;
    private final SpeechReportRepository speechReportRepository;

    @Transactional
    public SpeechReportCreateRes createReport(
            Long speechId,
            Long reporterUserId,
            SpeechReportCreateCommand command
    ) {
        Speech speech = speechRepository.findByIdAndDeletedFalse(speechId)
                .orElseThrow(() -> new CustomException(ErrorCode.SPEECH_NOT_FOUND));

        if (speech.getUserId().equals(reporterUserId)) {
            throw new CustomException(ErrorCode.SPEECH_REPORT_SELF_NOT_ALLOWED);
        }

        if (speechReportRepository.existsBySpeechIdAndReporterUserId(
                speechId,
                reporterUserId
        )) {
            throw new CustomException(ErrorCode.SPEECH_REPORT_ALREADY_EXISTS);
        }

        SpeechReport report = SpeechReport.create(
                speechId,
                speech.getUserId(),
                reporterUserId,
                speech.getContent(),
                command.reason(),
                command.description()
        );

        return SpeechReportCreateRes.from(speechReportRepository.save(report));
    }
}
