package com.sisibibi.api.domain.speechreport.config;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;

import java.lang.reflect.Constructor;

import static org.assertj.core.api.Assertions.assertThat;

class SpringAiOffTopicAiReviewerTest {

    @Test
    void constructorUsesOffTopicAiReviewTimeoutChatClient() {
        Constructor<?> constructor =
                SpringAiOffTopicAiReviewer.class.getConstructors()[0];

        Qualifier qualifier = constructor.getParameters()[0]
                .getAnnotation(Qualifier.class);

        assertThat(constructor.getParameterTypes()).containsExactly(ChatClient.class);
        assertThat(qualifier).isNotNull();
        assertThat(qualifier.value())
                .isEqualTo(OffTopicAiReviewerConfig.OFF_TOPIC_AI_REVIEW_CHAT_CLIENT);
    }
}
