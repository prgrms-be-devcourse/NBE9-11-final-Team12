package com.sisibibi.api.global.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@Profile("load-test")
public class LoadTestSecurityConfig {

    @Bean
    public SecurityFilterChain loadTestSecurityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/load-test/**").permitAll()
                        .requestMatchers("/api/v1/rooms/*/stage/**").permitAll()
                        .anyRequest().authenticated()
                )
                .build();
    }
}
