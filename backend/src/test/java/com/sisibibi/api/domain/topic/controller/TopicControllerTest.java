package com.sisibibi.api.domain.topic.controller;

import com.sisibibi.api.ApiApplication;
import com.sisibibi.api.domain.topic.dto.response.issueRes.IssueCandidateRes;
import com.sisibibi.api.domain.topic.dto.response.issueRes.IssueNewsRes;
import com.sisibibi.api.domain.topic.dto.response.topicRes.TopicDetailRes;
import com.sisibibi.api.domain.topic.dto.response.topicRes.TopicSummaryRes;
import com.sisibibi.api.domain.topic.service.TopicIssueService;
import com.sisibibi.api.domain.topic.service.TopicService;
import com.sisibibi.api.global.exception.CustomException;
import com.sisibibi.api.global.exception.ErrorCode;
import com.sisibibi.api.global.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TopicController.class)
@AutoConfigureMockMvc(addFilters = false)
@ContextConfiguration(classes = {
    ApiApplication.class,
    TopicController.class,
    GlobalExceptionHandler.class
})
class TopicControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private TopicIssueService topicIssueService;

  @MockitoBean
  private TopicService topicService;

  @Test
  void getTopicDetail_returnsOk() throws Exception {
    given(topicService.getTopicDetail(1L))
        .willReturn(new TopicDetailRes(
            1L,
            "AI 뉴스",
            "AI 뉴스 신뢰성 토론",
            "IT",
            "https://example.com",
            LocalDateTime.of(2026, 6, 16, 10, 0),
            LocalDateTime.of(2026, 6, 16, 10, 0)
        ));

    mockMvc.perform(get("/api/v1/topics/issues/{topicId}", 1L))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value(200))
        .andExpect(jsonPath("$.code").value("SUCCESS"))
        .andExpect(jsonPath("$.data.id").value(1))
        .andExpect(jsonPath("$.data.title").value("AI 뉴스"))
        .andExpect(jsonPath("$.data.category").value("IT"));

    verify(topicService).getTopicDetail(1L);
  }

  @Test
  void getTopicDetail_returnsNotFound_whenTopicDoesNotExist() throws Exception {
    given(topicService.getTopicDetail(999L))
        .willThrow(new CustomException(ErrorCode.TOPIC_NOT_FOUND));

    mockMvc.perform(get("/api/v1/topics/issues/{topicId}", 999L))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("TOPIC_NOT_FOUND"));
  }

  @Test
  void createIssue_returnsIssueCandidates() throws Exception {
    given(topicIssueService.createIssue())
        .willReturn(List.of(
            new IssueCandidateRes(
                "AI",
                1000L,
                20,
                List.of(
                    new IssueNewsRes(
                        "AI news",
                        "https://original.example.com",
                        "https://news.example.com",
                        "description",
                        "Tue, 16 Jun 2026 10:00:00 +0900"
                    )
                )
            )
        ));

    mockMvc.perform(get("/api/v1/topics/issues/candidates"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value(200))
        .andExpect(jsonPath("$.code").value("SUCCESS"))
        .andExpect(jsonPath("$.data[0].keyword").value("AI"))
        .andExpect(jsonPath("$.data[0].searchVolume").value(1000))
        .andExpect(jsonPath("$.data[0].increasePercentage").value(20))
        .andExpect(jsonPath("$.data[0].news[0].title").value("AI news"));

    verify(topicIssueService).createIssue();
  }

  @Test
  void getApprovedTopics_returnsPagedTopics() throws Exception {
    given(topicService.getApprovedTopics(org.mockito.ArgumentMatchers.any()))
        .willReturn(new PageImpl<>(
            List.of(
                new TopicSummaryRes(
                    1L,
                    "AI 뉴스",
                    "IT",
                    "https://example.com",
                    LocalDateTime.of(2026, 6, 16, 10, 0),
                    LocalDateTime.of(2026, 6, 16, 10, 0)
                )
            ),
            PageRequest.of(0, 20),
            1
        ));

    mockMvc.perform(get("/api/v1/topics/issues"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value(200))
        .andExpect(jsonPath("$.code").value("SUCCESS"))
        .andExpect(jsonPath("$.data.content[0].id").value(1))
        .andExpect(jsonPath("$.data.content[0].title").value("AI 뉴스"))
        .andExpect(jsonPath("$.data.content[0].category").value("IT"));

    verify(topicService).getApprovedTopics(org.mockito.ArgumentMatchers.any());
  }
}