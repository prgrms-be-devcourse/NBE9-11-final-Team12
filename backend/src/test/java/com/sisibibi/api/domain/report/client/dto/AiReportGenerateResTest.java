package com.sisibibi.api.domain.report.client.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AiReportGenerateResTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void deserializesKoreanBaseReportFieldsAndCustomReports() throws Exception {
        String json = """
                {
                  "\\uD575\\uC2EC \\uD55C\\uC904": "core",
                  "\\uD575\\uC2EC \\uC7C1\\uC810": ["issue"],
                  "AI \\uC885\\uD569 \\uC815\\uB9AC": "summary",
                  "\\uACF5\\uD1B5 \\uC758\\uACAC": "common",
                  "AI\\uC758 \\uAC1C\\uC778\\uC801 \\uC18C\\uACAC": "opinion",
                  "customReports": [
                    {
                      "label": "custom label",
                      "content": "custom content"
                    }
                  ]
                }
                """;

        AiReportGenerateRes response = objectMapper.readValue(json, AiReportGenerateRes.class);

        assertThat(response.coreLine()).isEqualTo("core");
        assertThat(response.keyIssues()).containsExactly("issue");
        assertThat(response.aiSummary()).isEqualTo("summary");
        assertThat(response.commonGround()).isEqualTo("common");
        assertThat(response.aiOpinion()).isEqualTo("opinion");
        assertThat(response.customReports()).containsExactly(
                new AiReportCustomReportPayload("custom label", "custom content")
        );
    }

    @Test
    void hasBaseRequiredFields_rejectsEmptyOrBlankKeyIssues() {
        assertThat(new AiReportGenerateRes("core", java.util.List.of(), "summary", "common", "opinion")
                .hasBaseRequiredFields()).isFalse();
        assertThat(new AiReportGenerateRes("core", java.util.List.of(" "), "summary", "common", "opinion")
                .hasBaseRequiredFields()).isFalse();
    }
}
