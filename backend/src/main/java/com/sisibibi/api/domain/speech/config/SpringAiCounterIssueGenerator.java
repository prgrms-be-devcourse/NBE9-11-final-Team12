package com.sisibibi.api.domain.speech.config;

import com.sisibibi.api.domain.room.entity.Room;
import com.sisibibi.api.domain.speech.entity.SpeechStance;
import java.net.http.HttpClient;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class SpringAiCounterIssueGenerator implements SpeechAiGenerator {

    private final ChatClient chatClient;

    public SpringAiCounterIssueGenerator(
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

        this.chatClient = ChatClient.builder(timeoutChatModel).build();
    }

    @Override
    public String generate(Room room, SpeechStance targetStance) {
        String stanceText = targetStance == SpeechStance.PRO ? "찬성" : "반대";

        String content = chatClient.prompt()
                .system("""
                        당신은 토론 균형을 돕는 AI 진행 보조자입니다.
                        한쪽 입장의 발언자가 연속으로 이어질 때, 반대 입장에서 생각해볼 쟁점을 제시합니다.
                        조건:
                        - 한국어로 작성
                        - 1개의 핵심 쟁점만 제시
                        - 300자 이내
                        - 인신공격, 혐오, 허위 사실 단정 금지
                        - 사용자의 발언처럼 쓰지 말고 토론 쟁점처럼 작성
                        """)
                .user(user -> user.text("""
                        토론방 제목: {roomTitle}
                        제시할 입장: {stance}

                        위 주제에 맞는 {stance} 측 쟁점을 제시해 주세요.
                        """)
                        .param("roomTitle", room.getTitle())
                        .param("stance", stanceText))
                .call()
                .content();

        if (content == null || content.isBlank()) {
            throw new IllegalStateException("AI counter issue content is blank.");
        }

        return content.trim();
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
