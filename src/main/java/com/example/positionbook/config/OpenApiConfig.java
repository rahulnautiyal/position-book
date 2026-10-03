package com.example.positionbook.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    OpenAPI positionBookOpenAPI() {
        return new OpenAPI().info(new Info()
                .title("Position Book API")
                .version("v1")
                .description("In-memory real-time position book for BUY, SELL and CANCEL trade events."));
    }
}
