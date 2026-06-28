package com.sisibibi.api.domain.report.service;

import com.sisibibi.api.domain.report.entity.AiReportCustomReport;
import com.sisibibi.api.domain.speech.entity.SpeechStance;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OpenHtmlAiReportPdfRendererTest {

    @Test
    void render_createsPdfBytes() {
        OpenHtmlAiReportPdfRenderer renderer = new OpenHtmlAiReportPdfRenderer();
        byte[] pdf = renderer.render(sampleModel());
        assertThat(pdf).isNotEmpty();
        assertThat(new String(pdf, 0, 4, StandardCharsets.ISO_8859_1)).isEqualTo("%PDF");
    }

    @Test
    void render_handlesEmptyTopOpinionLists() {
        OpenHtmlAiReportPdfRenderer renderer = new OpenHtmlAiReportPdfRenderer();
        AiReportPdfModel model = new AiReportPdfModel(
                1L, 10L, 20L,
                "빈 의견 테스트 방", "테스트 주제", "주제 설명",
                "user@test.com", "테스터",
                0, 0L, 0L, 0L, 0L,
                "핵심 한 줄", List.of(), "공통 의견", "AI 요약", "AI 의견",
                List.of(), List.of(), List.of(),
                LocalDateTime.now()
        );
        byte[] pdf = renderer.render(model);
        assertThat(pdf).isNotEmpty();
        assertThat(new String(pdf, 0, 4, StandardCharsets.ISO_8859_1)).isEqualTo("%PDF");
    }

    private AiReportPdfModel sampleModel() {
        List<AiReportPdfModel.TopOpinion> proOpinions = List.of(
                new AiReportPdfModel.TopOpinion(101L, 1L, "찬성유저", SpeechStance.PRO, "AI 기술 발전은 필수적입니다.", 5L, LocalDateTime.now())
        );
        List<AiReportPdfModel.TopOpinion> conOpinions = List.of(
                new AiReportPdfModel.TopOpinion(102L, 2L, "반대유저", SpeechStance.CON, "일자리 문제가 심각합니다.", 3L, LocalDateTime.now())
        );
        List<AiReportCustomReport> customReports = List.of(
                new AiReportCustomReport("경제적 영향 분석", "경제 측면에서 분석해줘", "경제 영향", "AI 도입으로 생산성은 향상되지만 초기 전환 비용이 상당합니다.")
        );
        return new AiReportPdfModel(
                1L, 10L, 20L,
                "AI 기술 토론방", "AI의 미래는 어떻게 될까?", "인공지능 발전 방향에 대한 토론",
                "requester@test.com", "요청자닉네임",
                10, 15L, 20L, 8L, 7L,
                "AI는 기회이자 도전이다",
                List.of("일자리 변화", "윤리적 문제", "기술 안전성"),
                "모두가 AI 규제의 필요성에 동의했다",
                "AI 기술은 사회 전반에 걸쳐 큰 변화를 가져올 것으로 예상됩니다.",
                "적절한 규제와 함께 발전시켜야 합니다.",
                proOpinions, conOpinions,
                customReports,
                LocalDateTime.now()
        );
    }
}
