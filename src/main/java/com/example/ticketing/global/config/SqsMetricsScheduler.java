package com.example.ticketing.global.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * worker 환경의 SQS 메시지 적체량을 주기적으로 조회하여
 * 그라파나 대시보드(Prometheus) 메트릭으로 노출하는 스케줄러.
 */
@Component
@Profile("worker")
@Slf4j
public class SqsMetricsScheduler {

    private final SqsAsyncClient sqsAsyncClient;
    private final String queueUrlOrName;
    private final String metricQueueName;
    private final AtomicInteger visibleMessages = new AtomicInteger(0);

    public SqsMetricsScheduler(
            SqsAsyncClient sqsAsyncClient,
            MeterRegistry meterRegistry,
            @Value("${app.sqs.booking-queue:booking-queue}") String queueUrlOrName) {
        this.sqsAsyncClient = sqsAsyncClient;
        this.queueUrlOrName = queueUrlOrName;
        this.metricQueueName = resolveQueueName(queueUrlOrName);

        // YACE/PrometheusRule이 사용하는 dimension_QueueName 라벨과 맞추기 위해
        // URL이 주입되더라도 마지막 segment의 실제 큐 이름만 라벨로 사용한다.
        meterRegistry.gauge("aws_sqs_approximate_number_of_messages_visible_average",
                Tags.of("dimension_QueueName", metricQueueName),
                this.visibleMessages,
                AtomicInteger::get);
    }

    @Scheduled(fixedRate = 2000) // 2초마다 큐 적체량 폴링
    public void pollQueueDepth() {
        try {
            resolveQueueUrl()
                    .thenCompose(queueUrl -> sqsAsyncClient.getQueueAttributes(GetQueueAttributesRequest.builder()
                            .queueUrl(queueUrl)
                            .attributeNames(QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES)
                            .build()))
                    .thenAccept(attributesResponse -> {
                        String countStr = attributesResponse.attributes().get(QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES);
                        if (countStr != null) {
                            int count = Integer.parseInt(countStr);
                            this.visibleMessages.set(count);
                        }
                    })
                    .exceptionally(ex -> {
                        log.warn("로컬 SQS 메트릭 조회 실패: {}", ex.getMessage());
                        return null;
                    });
        } catch (Exception e) {
            log.warn("SQS 메트릭 조회 스케줄러 실행 오류: {}", e.getMessage());
        }
    }

    private java.util.concurrent.CompletableFuture<String> resolveQueueUrl() {
        if (queueUrlOrName.startsWith("http://") || queueUrlOrName.startsWith("https://")) {
            return java.util.concurrent.CompletableFuture.completedFuture(queueUrlOrName);
        }

        return sqsAsyncClient.getQueueUrl(GetQueueUrlRequest.builder()
                        .queueName(queueUrlOrName)
                        .build())
                .thenApply(urlResponse -> urlResponse.queueUrl());
    }

    private String resolveQueueName(String queueUrlOrName) {
        int lastSlashIndex = queueUrlOrName.lastIndexOf('/');
        if (lastSlashIndex >= 0 && lastSlashIndex < queueUrlOrName.length() - 1) {
            return queueUrlOrName.substring(lastSlashIndex + 1);
        }
        return queueUrlOrName;
    }
}
