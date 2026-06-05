package com.babacar.payment.kafka;

import com.babacar.payment.domain.OutboxEvent;
import com.babacar.payment.service.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Outbox poller: reads unpublished events from the outbox table
 * and publishes them to Kafka. Runs every 500ms.
 *
 * This decouples DB writes from Kafka publishing,
 * guaranteeing at-least-once delivery even if Kafka is temporarily unavailable.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxPoller {

    private final OutboxEventRepository outboxEventRepository;
    private final PaymentEventProducer producer;

    @Scheduled(fixedDelay = 500)
    @Transactional
    public void pollAndPublish() {
        List<OutboxEvent> unpublished = outboxEventRepository.findByPublishedFalse();

        for (OutboxEvent event : unpublished) {
            try {
                producer.publish(event.getAggregateId(), event.getPayload());
                event.setPublished(true);
                outboxEventRepository.save(event);
                log.debug("Outbox event published: {} for aggregate {}", event.getId(), event.getAggregateId());
            } catch (Exception e) {
                log.error("Failed to publish outbox event: {}", event.getId(), e);
                // Will retry on next poll cycle
            }
        }
    }
}
