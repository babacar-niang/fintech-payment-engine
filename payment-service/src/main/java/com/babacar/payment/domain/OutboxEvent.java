package com.babacar.payment.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

/**
 * Outbox pattern: events are written to this table in the same DB transaction
 * as the payment. A scheduler polls and publishes them to Kafka.
 * This guarantees consistency between DB state and Kafka events.
 */
@Entity
@Table(name = "outbox_events")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OutboxEvent {

    @Id
    private UUID id;

    @Column(name = "aggregate_type", nullable = false)
    private String aggregateType;   // e.g. "Payment"

    @Column(name = "aggregate_id", nullable = false)
    private String aggregateId;     // payment UUID

    @Column(name = "event_type", nullable = false)
    private String eventType;       // e.g. "PaymentCreated"

    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
    private String payload;         // JSON

    @Column(name = "published", nullable = false)
    private boolean published;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    public void prePersist() {
        if (id == null) id = UUID.randomUUID();
        createdAt = Instant.now();
        published = false;
    }
}
