package com.sisibibi.api.domain.speech.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Constructor;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;

class SpringAiStageSummaryGeneratorTest {

    @Test
    void constructorUsesStageSummaryTimeoutChatClient() {
        Constructor<?> constructor =
                SpringAiStageSummaryGenerator.class.getConstructors()[0];

        Qualifier qualifier = constructor.getParameters()[0]
                .getAnnotation(Qualifier.class);

        assertThat(constructor.getParameterTypes()).containsExactly(ChatClient.class);
        assertThat(qualifier).isNotNull();
        assertThat(qualifier.value())
                .isEqualTo(StageSummaryGeneratorConfig.STAGE_SUMMARY_CHAT_CLIENT);
    }
}
