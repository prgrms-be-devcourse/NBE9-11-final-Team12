package com.sisibibi.api.global.config;

import com.sisibibi.api.global.security.config.AuthProperties;
import com.sisibibi.api.global.security.filter.JwtAuthenticationFilter;
import com.sisibibi.api.global.security.handler.SecurityExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableConfigurationProperties(AuthProperties.class)
public class SecurityConfig {

  private final JwtAuthenticationFilter jwtAuthenticationFilter;
  private final SecurityExceptionHandler securityExceptionHandler;
  private final String frontendUrl;

  public SecurityConfig(
      JwtAuthenticationFilter jwtAuthenticationFilter,
      SecurityExceptionHandler securityExceptionHandler,
      @Value("${app.frontend.url}") String frontendUrl
  ) {
    this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    this.securityExceptionHandler = securityExceptionHandler;
    this.frontendUrl = frontendUrl;
  }

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    return http
        .csrf(csrf -> csrf
            .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
            .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
            .ignoringRequestMatchers("/api/v1/auth/**")
        )
        .cors(cors -> cors.configurationSource(corsConfigurationSource()))
        .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .formLogin(formLogin -> formLogin.disable())
        .httpBasic(httpBasic -> httpBasic.disable())
        .logout(AbstractHttpConfigurer::disable)
        .exceptionHandling(exception -> exception
            .authenticationEntryPoint(securityExceptionHandler)
            .accessDeniedHandler(securityExceptionHandler)
        )
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
            .requestMatchers("/api/v1/topics/issues/**").permitAll()
            .requestMatchers("/api/v1/csrf").permitAll()
            .requestMatchers(
                "/actuator/health",
                "/actuator/prometheus"
            ).permitAll()
            .requestMatchers(
                "/api/v1/auth/**"
            ).permitAll()
            .anyRequest().authenticated()
        )
        .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
        .build();
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOrigins(List.of(frontendUrl));
    configuration.setAllowedMethods(List.of("GET", "POST", "PATCH", "PUT", "DELETE", "OPTIONS"));
    configuration.setAllowedHeaders(List.of("*"));
    configuration.setAllowCredentials(true);
    configuration.setMaxAge(3600L);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }
}
