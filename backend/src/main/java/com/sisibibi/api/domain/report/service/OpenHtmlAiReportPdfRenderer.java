package com.sisibibi.api.domain.report.service;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
public class OpenHtmlAiReportPdfRenderer implements AiReportPdfRenderer {

    @Override
    public byte[] render(AiReportPdfModel model) {
        try {
            String html = template()
                    .replace("{{roomTitle}}", escape(model.roomTitle()))
                    .replace("{{topicTitle}}", escape(model.topicTitle()))
                    .replace("{{coreLine}}", escape(model.coreLine()))
                    .replace("{{aiSummary}}", escape(model.aiSummary()))
                    .replace("{{aiOpinion}}", escape(model.aiOpinion()))
                    .replace("{{commonGround}}", escape(model.commonGround()))
                    .replace("{{participantCount}}", String.valueOf(model.participantCount()))
                    .replace("{{opinionCount}}", String.valueOf(model.opinionCount()))
                    .replace("{{reactionCount}}", String.valueOf(model.reactionCount()))
                    .replace("{{proPercent}}", String.valueOf(model.proPercent()))
                    .replace("{{conPercent}}", String.valueOf(model.conPercent()))
                    .replace("{{keyIssues}}", renderKeyIssues(model))
                    .replace("{{proOpinions}}", renderOpinions(model.proTopOpinions()))
                    .replace("{{conOpinions}}", renderOpinions(model.conTopOpinions()));

            ByteArrayOutputStream output = new ByteArrayOutputStream();
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(html, null);
            builder.useFont(() -> fontStream(), "Noto Sans KR");
            builder.toStream(output);
            builder.run();
            return output.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to render AI report PDF.", e);
        }
    }

    private InputStream fontStream() {
        try {
            return new ClassPathResource("fonts/NotoSansKR-Regular.ttf").getInputStream();
        } catch (Exception e) {
            throw new IllegalStateException("AI report PDF font resource is missing.", e);
        }
    }

    private String template() throws Exception {
        try (InputStream input = new ClassPathResource("templates/ai-report-pdf.html").getInputStream()) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private String renderKeyIssues(AiReportPdfModel model) {
        return model.keyIssues().stream()
                .map(issue -> "<li>" + escape(issue) + "</li>")
                .reduce("", String::concat);
    }

    private String renderOpinions(List<AiReportPdfModel.TopOpinion> opinions) {
        return opinions.stream()
                .map(opinion -> "<article class=\"opinion\"><p>" + escape(opinion.content())
                        + "</p><span>공감 " + opinion.reactionCount() + "</span></article>")
                .reduce("", String::concat);
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
