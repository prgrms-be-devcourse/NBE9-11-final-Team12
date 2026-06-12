package com.sisibibi.api.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

@Configuration
public class SecurityConfig {

    private static final String CREATE_SPEECH_PATH = "/api/v1/rooms/{roomId}/speeches";

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        RequestMatcher createSpeechMatcher = PathPatternRequestMatcher
                .withDefaults()
                .matcher(HttpMethod.POST, CREATE_SPEECH_PATH);

        return http
                .csrf(csrf -> csrf.ignoringRequestMatchers(createSpeechMatcher))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(createSpeechMatcher).permitAll()
                        .anyRequest().authenticated()
                )
                .build();
    }
}
