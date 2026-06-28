package com.sisibibi.api.domain.report.service;

import com.sisibibi.api.domain.report.entity.AiReportCustomReport;
import com.sisibibi.api.domain.speech.entity.SpeechStance;

import java.time.LocalDateTime;
import java.util.List;

public record AiReportPdfModel(
        Long exportId,
        Long reportId,
        Long roomId,
        String roomTitle,
        String topicTitle,
        String topicDescription,
        String requesterEmail,
        String requesterNickname,
        int participantCount,
        long opinionCount,
        long reactionCount,
        long proCount,
        long conCount,
        String coreLine,
        List<String> keyIssues,
        String commonGround,
        String aiSummary,
        String aiOpinion,
        List<TopOpinion> proTopOpinions,
        List<TopOpinion> conTopOpinions,
        List<AiReportCustomReport> customReports,
        LocalDateTime generatedAt
) {

    public long totalStanceCount() {
        return proCount + conCount;
    }

    public int proPercent() {
        return percent(proCount);
    }

    public int conPercent() {
        return percent(conCount);
    }

    private int percent(long count) {
        long total = totalStanceCount();
        if (total == 0) {
            return 0;
        }
        return (int) Math.round((count * 100.0) / total);
    }

    public record TopOpinion(
            Long speechId,
            Long userId,
            String nickname,
            SpeechStance stance,
            String content,
            long reactionCount,
            LocalDateTime createdAt
    ) {
    }
}
