package com.example.ticketing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Import;                          // ← 추가
import io.awspring.cloud.sqs.config.SqsBootstrapConfiguration; 

@SpringBootApplication
@EnableCaching
@Import(SqsBootstrapConfiguration.class)
public class TicketingApplication {
    public static void main(String[] args) {
        SpringApplication.run(TicketingApplication.class, args);
    }
}
