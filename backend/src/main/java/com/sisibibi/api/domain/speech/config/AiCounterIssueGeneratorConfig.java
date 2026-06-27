package com.sisibibi.api.domain.speech.config;

import java.net.http.HttpClient;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class AiCounterIssueGeneratorConfig {

    public static final String AI_COUNTER_ISSUE_CHAT_CLIENT = "aiCounterIssueChatClient";

    @Bean(name = AI_COUNTER_ISSUE_CHAT_CLIENT)
    public ChatClient aiCounterIssueChatClient(
            OpenAiApi openAiApi,
            OpenAiChatModel openAiChatModel,
            RestClient.Builder restClientBuilder,
            AiCounterIssueProperties properties
    ) {
        RestClient.Builder timeoutRestClientBuilder = restClientBuilder.clone()
                .requestFactory(createRequestFactory(properties));
        OpenAiApi timeoutOpenAiApi = openAiApi.mutate()
                .restClientBuilder(timeoutRestClientBuilder)
                .build();
        OpenAiChatModel timeoutChatModel = openAiChatModel.mutate()
                .openAiApi(timeoutOpenAiApi)
                .build();

        return ChatClient.builder(timeoutChatModel).build();
    }

    private JdkClientHttpRequestFactory createRequestFactory(
            AiCounterIssueProperties properties
    ) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.getGenerateTimeout())
                .build();

        JdkClientHttpRequestFactory requestFactory =
                new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(properties.getGenerateTimeout());
        return requestFactory;
    }
}
