package com.sisibibi.api.domain.topic.controller;

import com.sisibibi.api.ApiApplication;
import com.sisibibi.api.domain.topic.dto.response.issueRes.IssueNewsRes;
import com.sisibibi.api.domain.topic.dto.response.keywordres.ClassifiedIssueCandidateRes;
import com.sisibibi.api.domain.topic.dto.response.keywordres.ClassifiedIssueNewsRes;
import com.sisibibi.api.domain.topic.service.TopicCandidateService;
import com.sisibibi.api.domain.topic.service.TopicIssueService;
import com.sisibibi.api.domain.topic.service.TopicKeywordService;
import com.sisibibi.api.domain.topic.service.TopicService;
import com.sisibibi.api.global.exception.GlobalExceptionHandler;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminTopicController.class)
@AutoConfigureMockMvc(addFilters = false)
@ContextConfiguration(classes = {
    ApiApplication.class,
    AdminTopicController.class,
    GlobalExceptionHandler.class
})
class AdminTopicControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private TopicService topicService;

  @MockitoBean
  private TopicKeywordService topicKeywordService;

  @MockitoBean
  private TopicIssueService topicIssueService;

  @MockitoBean
  private TopicCandidateService topicCandidateService;

  @Test
  void getClassifiedCandidates_returnsCachedCandidates() throws Exception {
    given(topicCandidateService.getLatestClassifiedCandidates())
        .willReturn(List.of(classifiedCandidate()));

    mockMvc.perform(get("/api/v1/admin/topics/candidates/classified"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value(200))
        .andExpect(jsonPath("$.code").value("SUCCESS"))
        .andExpect(jsonPath("$.data[0].keyword").value("AI"))
        .andExpect(jsonPath("$.data[0].searchVolume").value(1000))
        .andExpect(jsonPath("$.data[0].increasePercentage").value(20))
        .andExpect(jsonPath("$.data[0].news[0].news.title").value("AI news"))
        .andExpect(jsonPath("$.data[0].news[0].category").value("IT"))
        .andExpect(jsonPath("$.data[0].news[0].keywords[0]").value("AI"));

    verify(topicCandidateService).getLatestClassifiedCandidates();
  }

  @Test
  void refreshClassifiedCandidates_returnsRefreshedCandidates() throws Exception {
    given(topicCandidateService.refreshLatestClassifiedCandidates())
        .willReturn(List.of(classifiedCandidate()));

    mockMvc.perform(post("/api/v1/admin/topics/candidates/classified/refresh"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value(200))
        .andExpect(jsonPath("$.code").value("SUCCESS"))
        .andExpect(jsonPath("$.data[0].keyword").value("AI"))
        .andExpect(jsonPath("$.data[0].news[0].news.title").value("AI news"))
        .andExpect(jsonPath("$.data[0].news[0].category").value("IT"));

    verify(topicCandidateService).refreshLatestClassifiedCandidates();
  }

  private ClassifiedIssueCandidateRes classifiedCandidate() {
    return new ClassifiedIssueCandidateRes(
        "AI",
        1000L,
        20,
        List.of(
            new ClassifiedIssueNewsRes(
                new IssueNewsRes(
                    "AI news",
                    "https://original.example.com",
                    "https://news.example.com",
                    "description",
                    "Tue, 16 Jun 2026 10:00:00 +0900"
                ),
                "IT",
                List.of("AI")
            )
        )
    );
  }
}
