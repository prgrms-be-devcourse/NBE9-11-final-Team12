package com.sisibibi.api.domain.topic.repository;

import com.sisibibi.api.domain.topic.entity.Topic;
import com.sisibibi.api.global.config.JpaAuditingConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@Import(JpaAuditingConfig.class)
class TopicRepositoryTest {

    @Autowired
    private TopicRepository topicRepository;

    @Test
    void save_assignsCreatedAtByJpaAuditing() {
        Topic topic = Topic.approved("주제", "설명", "카테고리", "https://example.com");

        assertThat(topic.getCreatedAt()).isNull();
        assertThat(topic.getApprovedAt()).isNotNull();

        Topic saved = topicRepository.saveAndFlush(topic);

        assertThat(saved.getCreatedAt()).isNotNull();
    }
}
