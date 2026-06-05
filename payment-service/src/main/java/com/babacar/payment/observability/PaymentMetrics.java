package com.babacar.payment.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * Custom business metrics exposed to Prometheus.
 * Available at /actuator/prometheus
 */
@Component
public class PaymentMetrics {

    private final Counter createdCounter;
    private final Counter completedCounter;
    private final Counter failedCounter;

    public PaymentMetrics(MeterRegistry registry) {
        createdCounter = Counter.builder("payments_created_total")
                .description("Total number of payments created")
                .register(registry);

        completedCounter = Counter.builder("payments_completed_total")
                .description("Total number of payments successfully completed")
                .register(registry);

        failedCounter = Counter.builder("payments_failed_total")
                .description("Total number of payments that failed")
                .register(registry);
    }

    public void incrementCreated()   { createdCounter.increment(); }
    public void incrementCompleted() { completedCounter.increment(); }
    public void incrementFailed()    { failedCounter.increment(); }
}
