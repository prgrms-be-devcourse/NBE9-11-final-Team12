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
public class StageSummaryGeneratorConfig {

    public static final String STAGE_SUMMARY_CHAT_CLIENT = "stageSummaryChatClient";

    @Bean(name = STAGE_SUMMARY_CHAT_CLIENT)
    public ChatClient stageSummaryChatClient(
            OpenAiApi openAiApi,
            OpenAiChatModel openAiChatModel,
            RestClient.Builder restClientBuilder,
            StageSummaryProperties properties
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
            StageSummaryProperties properties
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
