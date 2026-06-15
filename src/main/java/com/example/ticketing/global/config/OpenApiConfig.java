package com.example.ticketing.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        String securitySchemeName = "bearerAuth";
        
        return new OpenAPI()
                .info(new Info()
                        .title("Ticketing Service API")
                        .description("""
                                S-Tier High-Concurrency Ticketing Platform API 명세서입니다.

                                인증이 필요한 API는 우측 Authorize 버튼에 로그인 API로 발급받은 JWT를 입력해 호출합니다.
                                예매 요청 API는 JWT와 별도로 대기열 API에서 발급받은 Queue Token이 필요합니다.
                                """)
                        .version("v1.0.0"))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName,
                                new SecurityScheme()
                                        .name(securitySchemeName)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")));
    }
}
