package com.sisibibi.api.domain.speechreport.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;

@Configuration
public class OffTopicAiReviewerConfig {

    public static final String OFF_TOPIC_AI_REVIEW_CHAT_CLIENT =
            "offTopicAiReviewChatClient";

    @Bean(name = OFF_TOPIC_AI_REVIEW_CHAT_CLIENT)
    public ChatClient offTopicAiReviewChatClient(
            OpenAiApi openAiApi,
            OpenAiChatModel openAiChatModel,
            RestClient.Builder restClientBuilder,
            OffTopicAiReviewProperties properties
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
            OffTopicAiReviewProperties properties
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
