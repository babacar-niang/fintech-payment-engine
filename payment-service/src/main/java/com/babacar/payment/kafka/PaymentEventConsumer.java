package com.babacar.payment.kafka;

import com.babacar.payment.service.PaymentService;
import com.babacar.payment.service.dto.PaymentResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.TopicSuffixingStrategy;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

/**
 * Consumes payment events with:
 * - 3 retries with exponential backoff (1s → 2s → 4s)
 * - Automatic routing to DLQ after max retries
 * - Dead letter queue: payment-created-dlq
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventConsumer {

    private final PaymentService paymentService;
    private final ObjectMapper objectMapper;

    @RetryableTopic(
        attempts = "4",                     // 1 original + 3 retries
        backoff = @Backoff(
            delay = 1000,
            multiplier = 2.0,
            maxDelay = 10000
        ),
        topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE,
        dltTopicSuffix = "-dlq"
    )
    @KafkaListener(topics = PaymentEventProducer.TOPIC, groupId = "payment-processor")
    public void consume(String payload) {
        try {
            PaymentResponse event = objectMapper.readValue(payload, PaymentResponse.class);
            log.info("Processing payment event: {}", event.getId());

            // Simulate processing logic
            paymentService.markCompleted(event.getId());

        } catch (Exception e) {
            log.error("Failed to process payment event, will retry. Payload: {}", payload, e);
            throw new RuntimeException("Payment processing failed", e);
        }
    }

    /**
     * DLQ handler — messages land here after all retries are exhausted.
     * In production: alert on-call, store for manual replay.
     */
    @KafkaListener(topics = "payment-created-dlq", groupId = "payment-dlq-handler")
    public void handleDlq(String payload) {
        log.error("Payment event in DLQ — manual intervention required. Payload: {}", payload);
        // TODO: persist to dead_letter_payments table, trigger alert
    }
}
