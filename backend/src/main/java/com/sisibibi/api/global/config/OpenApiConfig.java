package com.sisibibi.api.global.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI sisibibiOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Sisibibi API")
                        .description("Sisibibi backend REST API documentation")
                        .version("v1"));
    }
}
