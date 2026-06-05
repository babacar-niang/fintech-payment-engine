package com.babacar.payment.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventProducer {

    public static final String TOPIC = "payment-created";

    private final KafkaTemplate<String, String> kafkaTemplate;

    public void publish(String paymentId, String payload) {
        kafkaTemplate.send(TOPIC, paymentId, payload)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish payment event for paymentId={}", paymentId, ex);
                    } else {
                        log.debug("Published payment event: paymentId={} offset={}",
                                paymentId, result.getRecordMetadata().offset());
                    }
                });
    }
}
